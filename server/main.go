package main

import (
	"log"
	"time"

	"driveon/render"

	"github.com/veandco/go-sdl2/sdl"
)

const (
	screenWidth  = 1024
	screenHeight = 600
)

func main() {
	r, err := render.New(screenWidth, screenHeight)
	if err != nil {
		log.Fatal(err)
	}
	defer r.Destroy()

	running := true

	for running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				running = false
			}
		}

		if err := r.Draw(); err != nil {
			log.Println(err)
		}

		time.Sleep(time.Millisecond * 16) // ~60 FPS
	}
}
