package main

import (
	"log"
	"math/rand"
	"strconv"
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

	r.SetInfos([]render.Info{
		render.NewInfo("Speed", "120km/h"),
		render.NewInfo("RPM", "3000"),
		render.NewInfo("Fuel", "50%"),
		render.NewInfo("Temp", "90°C"),
		render.NewInfo("Gear", "D"),
	})

	running := true

	for running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				running = false
			}
		}
		updateInfos(r)

		if err := r.Draw(); err != nil {
			log.Println(err)
		}

		time.Sleep(time.Millisecond * 16) // ~60 FPS
	}
}

func updateInfos(r *render.Renderer) {
	r.Infos[0].SetValue(strconv.Itoa(rand.Intn(200)) + "km/h")
	r.Infos[1].SetValue(strconv.Itoa(rand.Intn(7000)) + " RPM")
	r.Infos[2].SetValue(strconv.Itoa(rand.Intn(100)) + "%")
	r.Infos[3].SetValue(strconv.Itoa(rand.Intn(120)) + "°C")

}
