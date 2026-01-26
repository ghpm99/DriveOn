package main

import (
	"fmt"
	"log"
	"math/rand"

	"driveon/net"
	"driveon/render"

	"github.com/veandco/go-sdl2/sdl"
)

const (
	screenWidth  = 1024
	screenHeight = 600
)

type Scene struct {
	speedInfo, rpmInfo, fuelInfo, tempInfo *render.Info
}

type SmoothValue struct {
	Value int
	Min   int
	Max   int
	Step  int
}

type DriveOn struct {
	render  *render.Renderer
	server  *net.Server
	running bool
	scene   Scene
}

func main() {
	driveOnApp := &DriveOn{
		running: false,
	}
	driveOnApp.run()
}

func (driveON *DriveOn) run() {

	defer driveON.destroy()

	r, err := render.New(screenWidth, screenHeight)
	if err != nil {
		log.Fatal(err)
	}
	go startServer()

	driveON.render = r

	speed := render.NewInfo("Speed", "120 km/h")
	rpm := render.NewInfo("RPM", "3000")
	fuel := render.NewInfo("Fuel", "50%")
	temp := render.NewInfo("Temp", "90°C")
	gear := render.NewInfo("Gear", "D")

	driveON.scene = Scene{
		speedInfo: speed,
		rpmInfo:   rpm,
		fuelInfo:  fuel,
		tempInfo:  temp,
	}

	r.SetInfos([]*render.Info{
		speed,
		rpm,
		fuel,
		temp,
		gear,
	})

	driveON.running = true
	go driveON.sendFrameToDisplay()

	driveON.mainLoop()
}

func startServer() {
	err := net.Start()
	if err != nil {
		log.Fatal(err)
	}
}

func (driveON *DriveOn) sendFrameToDisplay() {
	for frame := range driveON.render.FrameBuffer {
		log.Println("Enviando frame")
		net.SendFrameToDisplay(frame)
	}
}

func (driveON *DriveOn) mainLoop() {
	for driveON.running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				driveON.running = false
			}
		}
		driveON.updateInfos()

		if err := driveON.render.Draw(); err != nil {
			log.Println(err)
		}

		log.Println("Lendo frame")
		err := driveON.render.ReadScreen()

		if err != nil {
			log.Println("Erro ao capturar tela:", err)
		}

	}
}

func (driveON *DriveOn) updateInfos() {
	var (
		speed = SmoothValue{Value: 80, Min: 0, Max: 240, Step: 2}
		rpm   = SmoothValue{Value: 2500, Min: 800, Max: 7000, Step: 150}
		fuel  = SmoothValue{Value: 50, Min: 0, Max: 100, Step: 1}
		temp  = SmoothValue{Value: 85, Min: 70, Max: 120, Step: 1}
	)

	speed.Update()
	rpm.Update()
	fuel.Update()
	temp.Update()

	driveON.scene.speedInfo.SetValue(fmt.Sprintf("%d km/h", speed.Value))
	driveON.scene.rpmInfo.SetValue(fmt.Sprintf("%d RPM", rpm.Value))
	driveON.scene.fuelInfo.SetValue(fmt.Sprintf("%d%%", fuel.Value))
	driveON.scene.tempInfo.SetValue(fmt.Sprintf("%d°C", temp.Value))
}

func (s *SmoothValue) Update() {
	delta := rand.Intn(s.Step*2+1) - s.Step // -step .. +step
	s.Value += delta

	if s.Value < s.Min {
		s.Value = s.Min
	}
	if s.Value > s.Max {
		s.Value = s.Max
	}
}

func (driveON *DriveOn) destroy() {
	if driveON.render != nil {
		driveON.render.Destroy()
	}

}
