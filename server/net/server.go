package net

import (
	"bufio"
	"bytes"
	"driveon/render"
	"encoding/binary"
	"fmt"
	"log"
	"net"
	"sync/atomic"
)

type Server struct {
	listener *net.Listener
	client   *Client
}

type Client struct {
	conn        net.Conn
	sendChannel chan []byte
	alive       atomic.Bool
	ackChan     chan struct{}
	frameBuffer chan render.Frame
}

var currentClient atomic.Pointer[Client]

func Start() error {
	// TODO:
	// - TCP listener
	// - receber eventos touch
	// - enviar frames

	listener, err := net.Listen("tcp", ":9000")
	if err != nil {
		return err
	}
	defer listener.Close()
	fmt.Println("Servidor aguardando tablet na porta 9000...")

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Println("Erro ao aceitar conexão:", err)
			continue
		}

		fmt.Println("Tablet conectado:", conn.RemoteAddr())
		client := &Client{
			conn:        conn,
			sendChannel: make(chan []byte, 4),
			ackChan:     make(chan struct{}, 1),
			frameBuffer: make(chan render.Frame, 2),
		}

		client.alive.Store(true)

		currentClient.Store(client)

		go handleRead(client)
		go handleWrite(client)
	}

}

func handleRead(c *Client) {
	defer c.conn.Close()

	reader := bufio.NewReader(c.conn)

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			log.Println("Cliente desconectou (read)")
			c.alive.Store(false)
			return
		}

		log.Print("Recebido: ", line)
		if line == "ACK\n" {
			log.Println("postando no channel")
			select {
			case c.ackChan <- struct{}{}:
			default:

			}
		}
		// TODO: parse TOUCH, comandos, etc
	}
}

func handleWrite(c *Client) {
	defer c.conn.Close()

	for frame := range c.sendChannel {
		_, err := c.conn.Write(frame)
		if err != nil {
			log.Println("Erro enviando frame:", err)
			c.alive.Store(false)
			return
		}
	}
}

func (c *Client) sendFrame(frame render.Frame) {

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

	select {
	case c.sendChannel <- buf.Bytes():
	default:
	}
	log.Println("Aguardando ACK")
	<-c.ackChan
}

func SendFrameToDisplay(frame render.Frame) {
	client := currentClient.Load()
	if client != nil && client.alive.Load() {
		client.sendFrame(frame)
	}
}
