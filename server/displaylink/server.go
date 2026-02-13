package displaylink

import (
	"bufio"
	"driveon/dtos"
	"encoding/binary"
	"fmt"
	"log"
	"net"
	"strconv"
	"strings"
	"time"
)

// ConnectionManager gerencia a conexão com o tablet
type ConnectionManager struct {
	Address string
	// Canais para comunicação com o resto do app (sem locks!)
	FrameInput   chan dtos.Frame      // Recebe imagens do render
	TouchOutput  chan dtos.TouchEvent // Envia toques para o main
	SensorOutput chan dtos.TelemetryData
}

func New(ip, port string) *ConnectionManager {
	return &ConnectionManager{
		Address:      ip + ":" + port,
		FrameInput:   make(chan dtos.Frame, 5), // Buffer pequeno para não acumular lag
		TouchOutput:  make(chan dtos.TouchEvent, 50),
		SensorOutput: make(chan dtos.TelemetryData, 5),
	}
}

// Start inicia o loop eterno de conexão. Rode isso em uma goroutine.
func (cm *ConnectionManager) Start() {
	for {
		log.Printf("Conectando a %s...", cm.Address)
		conn, err := net.DialTimeout("tcp", cm.Address, 2*time.Second)
		if err != nil {
			time.Sleep(1 * time.Second)
			continue
		}

		// if err := cm.handshake(conn); err != nil {
		// 	log.Printf("Handshake falhou: %v", err)
		// 	conn.Close()
		// 	time.Sleep(1 * time.Second)
		// 	continue
		// }

		// Contexto de erro para sincronizar a queda
		errChan := make(chan error, 2)

		// Inicia loops
		go cm.readLoop(conn, errChan)
		go cm.writeLoop(conn, errChan)

		// Bloqueia até QUE UM DOS DOIS falhe
		failErr := <-errChan
		log.Printf("Conexão perdida: %v", failErr)

		conn.Close() // Garante que o outro loop também morra
		cm.drainFrames()
	}
}

// handshake simples e linear
func (cm *ConnectionManager) handshake(conn net.Conn) error {
	// 1. Envia HELLO
	_, err := conn.Write([]byte("HELLO\n"))
	if err != nil {
		return err
	}

	// 2. Define timeout para não ficar travado aqui
	conn.SetReadDeadline(time.Now().Add(2 * time.Second))

	// 3. Espera WELCOME
	reader := bufio.NewReader(conn)
	resp, err := reader.ReadString('\n')
	if err != nil {
		return err
	}

	if resp != "WELCOME\n" {
		return fmt.Errorf("protocolo inválido: recebeu %s", resp)
	}

	// Remove o timeout para o funcionamento normal
	conn.SetReadDeadline(time.Time{})
	return nil
}

func (cm *ConnectionManager) writeLoop(conn net.Conn, errChan chan<- error) {
	headerBuf := make([]byte, 16)
	writer := bufio.NewWriterSize(conn, 4096) // Bufferiza escritas para reduzir syscalls

	for frameData := range cm.FrameInput {
		// Cabeçalho
		binary.BigEndian.PutUint32(headerBuf[0:], 0xDEADBEEF)
		binary.BigEndian.PutUint32(headerBuf[4:], uint32(frameData.Width))  // Width (Exemplo)
		binary.BigEndian.PutUint32(headerBuf[8:], uint32(frameData.Height)) // Height
		binary.BigEndian.PutUint32(headerBuf[12:], uint32(frameData.FrameSize))

		// Escreve no buffer interno (rápido)
		if _, err := writer.Write(headerBuf); err != nil {
			errChan <- err
			return
		}
		if _, err := writer.Write(frameData.Data); err != nil {
			errChan <- err
			return
		}

		// Envia tudo para a rede de uma vez
		if err := writer.Flush(); err != nil {
			errChan <- err
			return
		}
	}
}

func (cm *ConnectionManager) readLoop(conn net.Conn, errChan chan<- error) {
	reader := bufio.NewReader(conn)

	for {
		// Lê até o \n enviado pelo Android

		line, err := reader.ReadString('\n')

		if err != nil {
			errChan <- err
			return
		}

		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		// Roteamento de Pacotes
		if strings.HasPrefix(line, "TOUCH") {
			cm.parseTouch(line)
		} else if strings.HasPrefix(line, "T,") {
			cm.parseTelemetry(line)
		}
	}
}

func (cm *ConnectionManager) parseTouch(line string) {
	// Formato: TOUCH 100 200 0
	parts := strings.Split(line, " ")
	if len(parts) < 4 {
		return
	}

	x, _ := strconv.ParseFloat(parts[1], 32)
	y, _ := strconv.ParseFloat(parts[2], 32)
	action, _ := strconv.Atoi(parts[3])

	// Envia para o canal (Non-blocking para não travar leitura se o main estiver lento)
	select {
	case cm.TouchOutput <- dtos.TouchEvent{X: float32(x), Y: float32(y), Action: action}:
	default:
		// Se o canal encher, dropa o touch mais antigo (melhor que lagar)
	}
}

func (cm *ConnectionManager) parseTelemetry(line string) {
	// Formato: T,ax,ay,az,luz,lat,lon,spd
	parts := strings.Split(line, ",")
	if len(parts) < 5 {
		return
	} // Garante mínimo de sensores

	data := dtos.TelemetryData{}

	// Parse seguro (ignora erros para não crashar)
	data.AccX, _ = strconv.ParseFloat(parts[1], 64)
	data.AccY, _ = strconv.ParseFloat(parts[2], 64)
	data.AccZ, _ = strconv.ParseFloat(parts[3], 64)
	data.Light, _ = strconv.ParseFloat(parts[4], 64)

	// Se tiver GPS (Android enviou mais dados)
	if len(parts) >= 8 {
		data.Lat, _ = strconv.ParseFloat(parts[5], 64)
		data.Lon, _ = strconv.ParseFloat(parts[6], 64)
		data.Speed, _ = strconv.ParseFloat(parts[7], 64)
		data.HasGPS = true
	}

	// Atualiza o dado mais recente (substitui o antigo se houver)
	select {
	case <-cm.SensorOutput: // Esvazia slot velho se tiver
	default:
	}
	cm.SensorOutput <- data
}

func (cm *ConnectionManager) drainFrames() {
	// Esvazia o canal de frames pendentes para começar limpo
	for len(cm.FrameInput) > 0 {
		<-cm.FrameInput
	}
}
