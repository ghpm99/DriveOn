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
}

var CurrentClient atomic.Pointer[Client]

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
		}

		client.alive.Store(true)

		CurrentClient.Store(client)

		go handshake(client)
	}

}

func handshake(client *Client) {
	reader := bufio.NewReader(client.conn)
	line, err := reader.ReadString('\n')
	if err != nil {
		return
	}

	if line != "HELLO\n" {
		log.Printf("handshake falhou, mensagem inesperada: %s", line)
		return
	}

	log.Println("Handshake recebido, enviando WELCOME")

	_, err = client.conn.Write([]byte("WELCOME\n"))
	if err != nil {
		log.Println("Erro enviando WELCOME:", err)
		return
	}
	log.Println("Handshake completo, iniciando comunicação com o tablet")
	go handleRead(client)
	go handleWrite(client)
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
		// TODO: parse TOUCH, comandos, etc
	}
}

func handleWrite(c *Client) {
	defer c.conn.Close()

	for frame := range c.sendChannel {
		log.Println("Enviando frame, tamanho:", len(frame))
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

	c.sendChannel <- buf.Bytes()
}

func SendFrameToDisplay(frame render.Frame) {
	client := CurrentClient.Load()
	if client != nil && client.alive.Load() {
		client.sendFrame(frame)
	}
}
