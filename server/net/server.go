package net

import (
	"bufio"
	"fmt"
	"log"
	"net"
)

type Server struct {
	listener *net.Listener
}

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
		go handleClient(conn)
	}

}

func handleClient(conn net.Conn) {
	defer conn.Close()

	reader := bufio.NewReader(conn)

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			fmt.Println("Cliente desconectou")
			return
		}

		fmt.Print("Recebido: ", line)
	}
}
