package net

import (
	"bufio"
	"bytes"
	"driveon/render"
	"encoding/binary"
	"fmt"
	"log"
	"net"
	"sync"
	"sync/atomic"
	"time"
)

type Server struct {
	listener *net.Listener
	display  *Display
}

type Display struct {
	addr        string
	conn        net.Conn
	sendChannel chan []byte
	mu          sync.RWMutex
}

var DisplayClient atomic.Pointer[Display]

var displayIp = "192.168.42.129"
var displayPort = "9000"

func NewDisplay() *Display {
	return &Display{addr: displayIp + ":" + displayPort, sendChannel: make(chan []byte, 10)}
}

func (td *Display) Start() error {
	for {

		conn, err := net.DialTimeout("tcp", td.addr, 2*time.Second)
		if err != nil {
			fmt.Printf("Aguardando tablet em %s...\n", td.addr)
			time.Sleep(2 * time.Second)
			continue
		}

		if err := td.handshake(); err != nil {
			fmt.Println("Falha no handshake:", err)
			conn.Close()
			time.Sleep(2 * time.Second)
			continue
		}

		// 3. Sucesso! Guarda a conexão
		td.mu.Lock()
		td.conn = conn
		td.mu.Unlock()

		fmt.Println("Conectado e pronto para streaming!")

		// 4. Inicia a escuta de sensores (bloqueia aqui até cair)

		// Se chegou aqui, a conexão caiu
		td.mu.Lock()
		td.conn = nil
		td.mu.Unlock()

	}

}

func (client *Display) handshake() error {
	reader := bufio.NewReader(client.conn)
	line, err := reader.ReadString('\n')
	if err != nil {
		return err
	}

	if line != "HELLO\n" {
		return fmt.Errorf("handshake falhou, mensagem inesperada: %s", line)

	}

	log.Println("Handshake recebido, enviando WELCOME")

	_, err = client.conn.Write([]byte("WELCOME\n"))
	if err != nil {
		log.Println("Erro enviando WELCOME:", err)
		return err
	}
	log.Println("Handshake completo, iniciando comunicação com o tablet")
	go client.handleRead()
	go handleWrite(client)
	return nil
}

func (td *Display) handleRead() {
	defer td.conn.Close()

	for {
		td.mu.RLock()
		c := td.conn
		td.mu.RUnlock()
		if c == nil {
			log.Println("Conexão perdida, parando leitura")
			return
		}
		reader := bufio.NewReader(c)
		line, err := reader.ReadString('\n')
		if err != nil {
			log.Println("Cliente desconectou (read)")

			return
		}

		log.Print("Recebido: ", line)
		// TODO: parse TOUCH, comandos, etc
	}
}

func handleWrite(c *Display) {
	defer c.conn.Close()

	for frame := range c.sendChannel {
		if c.conn == nil {
			return
		}
		log.Println("Enviando frame, tamanho:", len(frame))
		_, err := c.conn.Write(frame)
		if err != nil {
			log.Println("Erro enviando frame:", err)

			return
		}
	}
}

func (c *Display) sendFrame(frame render.Frame) {

	var buf bytes.Buffer
	log.Println("Tamanho msg:", frame.FrameSize+12)

	log.Println("Escrevendo width")
	binary.Write(&buf, binary.BigEndian, frame.Width)
	log.Println("Escrevendo height")
	binary.Write(&buf, binary.BigEndian, frame.Height)
	log.Println("Escrevendo frameSize")
	binary.Write(&buf, binary.BigEndian, frame.FrameSize)
	log.Println("Escrevendo data")
	buf.Write(frame.Data)

	c.sendChannel <- buf.Bytes()
}

func SendFrameToDisplay(frame render.Frame) {
	client := DisplayClient.Load()
	if client != nil && client.conn != nil {
		client.sendFrame(frame)
	}
}

func (c *Display) Close() {
	close(c.sendChannel)
	c.conn.Close()
}
