package displaylink

import (
	"bufio"
	"driveon/dtos"
	"encoding/binary"
	"fmt"
	"log"
	"net"
	"time"
)

// ConnectionManager gerencia a conexão com o tablet
type ConnectionManager struct {
	Address string
	// Canais para comunicação com o resto do app (sem locks!)
	FrameInput  chan dtos.Frame      // Recebe imagens do render
	TouchOutput chan dtos.TouchEvent // Envia toques para o main
}

func New(ip, port string) *ConnectionManager {
	return &ConnectionManager{
		Address:     ip + ":" + port,
		FrameInput:  make(chan dtos.Frame, 5), // Buffer pequeno para não acumular lag
		TouchOutput: make(chan dtos.TouchEvent, 10),
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

		if err := cm.handshake(conn); err != nil {
			log.Printf("Handshake falhou: %v", err)
			conn.Close()
			time.Sleep(1 * time.Second)
			continue
		}

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
		line, err := reader.ReadString('\n')
		if err != nil {
			errChan <- err
			return
		}
		// TODO: Parse real do TouchEvent
		log.Printf("RX: %s", line)
	}
}

func (cm *ConnectionManager) drainFrames() {
	// Esvazia o canal de frames pendentes para começar limpo
	for len(cm.FrameInput) > 0 {
		<-cm.FrameInput
	}
}
