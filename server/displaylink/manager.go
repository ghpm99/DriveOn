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

type ConnectionManager struct {
	Address      string
	FrameInput   chan dtos.Frame
	TouchOutput  chan dtos.TouchEvent
	SensorOutput chan dtos.TelemetryData
}

// Structs que mapeiam EXATAMENTE os ByteBuffers do Java (Little Endian)
type FastSensorPacket struct {
	AccX, AccY, AccZ float32
	MagX, MagY, MagZ float32
}

type SlowSensorPacket struct {
	Light   float32
	Battery int32
	Lat     float64 // 8 bytes em Java (double)
	Lon     float64
	Speed   float32 // Float simples em java
	FPS     int32
}

func New(ip, port string) *ConnectionManager {
	return &ConnectionManager{
		Address:      ip + ":" + port,
		FrameInput:   make(chan dtos.Frame, 5),
		TouchOutput:  make(chan dtos.TouchEvent, 50),
		SensorOutput: make(chan dtos.TelemetryData, 5),
	}
}

func (cm *ConnectionManager) Start() {
	for {
		log.Printf("Aguardando conexão em %s...", cm.Address)

		// Como o tablet é o Servidor agora (ServerSocket na porta 9000), o PC tenta conectar
		conn, err := net.DialTimeout("tcp", cm.Address, 2*time.Second)
		if err != nil {
			time.Sleep(1 * time.Second)
			continue
		}

		log.Println("Conectado ao Tablet!")

		errChan := make(chan error, 2)

		go cm.readLoop(conn, errChan)
		go cm.writeLoop(conn, errChan)

		failErr := <-errChan
		log.Printf("Conexão perdida: %v", failErr)

		conn.Close()
		cm.drainFrames()
	}
}

func (cm *ConnectionManager) writeLoop(conn net.Conn, errChan chan<- error) {
	headerBuf := make([]byte, 16)
	writer := bufio.NewWriterSize(conn, 65536) // Buffer grande para video

	for frameData := range cm.FrameInput {
		// BigEndian aqui pois o DataInputStream.readInt() do Java usa BigEndian por padrão
		binary.BigEndian.PutUint32(headerBuf[0:], 0xDEADBEEF)
		binary.BigEndian.PutUint32(headerBuf[4:], uint32(frameData.Width))
		binary.BigEndian.PutUint32(headerBuf[8:], uint32(frameData.Height))
		binary.BigEndian.PutUint32(headerBuf[12:], uint32(frameData.FrameSize))

		if _, err := writer.Write(headerBuf); err != nil {
			errChan <- err
			return
		}
		if _, err := writer.Write(frameData.Data); err != nil {
			errChan <- err
			return
		}
		if err := writer.Flush(); err != nil {
			errChan <- err
			return
		}
	}
}

func (cm *ConnectionManager) readLoop(conn net.Conn, errChan chan<- error) {
	reader := bufio.NewReaderSize(conn, 4096)

	for {
		// 1. Lê o Cabeçalho (1 Byte = Tipo da Mensagem)
		msgType, err := reader.ReadByte()
		if err != nil {
			errChan <- err
			return
		}

		// Roteia baseado no Tipo
		switch msgType {
		case 0x01: // TOUCH EVENT (12 bytes de payload)
			var x, y float32
			var action int32

			// O Java enviou ByteBuffer em LITTLE_ENDIAN
			binary.Read(reader, binary.LittleEndian, &x)
			binary.Read(reader, binary.LittleEndian, &y)
			binary.Read(reader, binary.LittleEndian, &action)

			select {
			case cm.TouchOutput <- dtos.TouchEvent{X: x, Y: y, Action: int(action)}:
			default:
			}

		case 0x02: // FAST SENSORS (24 bytes de payload)
			var pkt FastSensorPacket
			// Lê a struct inteira da rede de uma só vez (MUITO eficiente)
			err := binary.Read(reader, binary.LittleEndian, &pkt)
			if err != nil {
				errChan <- err
				return
			}

			cm.SensorOutput <- dtos.TelemetryData{
				Type: "FAST",
				AccX: float64(pkt.AccX), AccY: float64(pkt.AccY), AccZ: float64(pkt.AccZ),
				MagX: float64(pkt.MagX), MagY: float64(pkt.MagY), MagZ: float64(pkt.MagZ),
			}

		case 0x03: // SLOW SENSORS (32 bytes de payload)
			var pkt SlowSensorPacket
			err := binary.Read(reader, binary.LittleEndian, &pkt)
			if err != nil {
				errChan <- err
				return
			}

			cm.SensorOutput <- dtos.TelemetryData{
				Type:    "SLOW",
				Light:   float64(pkt.Light),
				Battery: int(pkt.Battery),
				Lat:     pkt.Lat,
				Lon:     pkt.Lon,
				Speed:   float64(pkt.Speed),
				FPS:     int(pkt.FPS),
				HasGPS:  pkt.Lat != 0 || pkt.Lon != 0,
			}

		default:
			// Se o byte não é conhecido, a sincronia binária foi perdida.
			// É melhor fechar e reconectar.
			log.Printf("Protocolo corrompido. Byte desconhecido: 0x%X", msgType)
			errChan <- fmt.Errorf("protocol dessync")
			return
		}
	}
}

func (cm *ConnectionManager) drainFrames() {
	for len(cm.FrameInput) > 0 {
		<-cm.FrameInput
	}
}
