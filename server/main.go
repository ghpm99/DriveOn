package main

import (
	"fmt"
	"log"
	"time"

	"driveon/displaylink"
	"driveon/dtos"
	"driveon/render"

	"github.com/veandco/go-sdl2/sdl"
)

const (
	screenWidth  = 1024
	screenHeight = 600
)

type Scene struct {
	accXInfo,
	accYInfo,
	accZInfo,
	latInfo,
	longInfo,
	speedInfo,
	lightInfo *render.Info
}

type SmoothValue struct {
	Value int
	Min   int
	Max   int
	Step  int
}

type DriveOn struct {
	render           *render.Renderer
	displaylink      *displaylink.ConnectionManager
	running          bool
	scene            Scene
	cursorX, cursorY int32
	isTouching       bool
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

	driveON.displaylink = displaylink.New("192.168.42.129", "9000")
	go driveON.displaylink.Start()

	driveON.render = r

	speed := render.NewInfo("Speed", "0 km/h")
	accX := render.NewInfo("AccX", "0")
	accY := render.NewInfo("AccY", "0")
	accZ := render.NewInfo("AccZ", "0")
	lat := render.NewInfo("Lat", "0")
	lon := render.NewInfo("Lon", "0")
	light := render.NewInfo("Iluminação", "0")

	driveON.scene = Scene{
		speedInfo: speed,
		lightInfo: light,
		accXInfo:  accX,
		accYInfo:  accY,
		accZInfo:  accZ,
		latInfo:   lat,
		longInfo:  lon,
	}

	r.SetInfos([]*render.Info{
		speed,
		light,
		accX,
		accY,
		accZ,
		lat,
		lon,
	})

	driveON.running = true
	go driveON.sendFrameToDisplay()

	driveON.mainLoop()
}

func (driveON *DriveOn) sendFrameToDisplay() {
	for frame := range driveON.render.FrameBuffer {
		select {
		case driveON.displaylink.FrameInput <- frame:
		default:
		}
	}
}

func (driveON *DriveOn) mainLoop() {
	ticker := time.NewTicker(time.Second / 60)
	defer ticker.Stop()

	for driveON.running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				driveON.running = false
			}
		}
		driveON.processNetworkEvents()

		driveON.updateScene()

		if err := driveON.render.Draw(); err != nil {
			log.Println(err)
		}

		// 5. Captura tela para enviar de volta
		select {
		case <-ticker.C:
			err := driveON.render.ReadScreen()
			if err != nil {
				log.Println(err)
			}
		default:
		}

	}
}

func (driveON *DriveOn) processNetworkEvents() {
	// Loop para drenar todos os eventos pendentes neste frame
	// Isso garante que o touch seja suave e não acumule
	for {
		select {
		case touch := <-driveON.displaylink.TouchOutput:
			driveON.handleTouch(touch)

		case telemetry := <-driveON.displaylink.SensorOutput:
			driveON.handleTelemetry(telemetry)

		default:
			return // Nada mais para ler
		}
	}
}

func (driveON *DriveOn) handleTouch(t dtos.TouchEvent) {
	// Mapeia coordenadas (Se o tablet e PC tiverem resoluções diferentes, ajuste aqui)
	// Assumindo 1:1 por enquanto

	switch t.Action {
	case 0: // ACTION_DOWN
		driveON.isTouching = true
		// Opcional: Injetar clique no SDL
		// sdl.PushEvent(&sdl.MouseButtonEvent{Type: sdl.MOUSEBUTTONDOWN, Button: sdl.BUTTON_LEFT, X: driveON.cursorX, Y: driveON.cursorY})
	case 1: // ACTION_UP
		driveON.isTouching = false
		// sdl.PushEvent(&sdl.MouseButtonEvent{Type: sdl.MOUSEBUTTONUP, ...})
	case 2: // ACTION_MOVE
		// Apenas atualiza coordenadas (já feito acima)
	}

	driveON.render.TouchCursor <- t // Envia para o render desenhar o cursor
}

func (driveON *DriveOn) handleTelemetry(t dtos.TelemetryData) {
	// Atualiza a Scene com dados reais!

	driveON.scene.accXInfo.SetValue(fmt.Sprintf("%f", t.AccX))
	driveON.scene.accYInfo.SetValue(fmt.Sprintf("%f", t.AccY))
	driveON.scene.accZInfo.SetValue(fmt.Sprintf("%f", t.AccZ))

	// Luz vira "Faróis" ou brilho
	driveON.scene.lightInfo.SetValue(fmt.Sprintf("%.0f%%", t.Light))

	if t.HasGPS {
		driveON.scene.latInfo.SetValue(fmt.Sprintf("%.5f", t.Lat))
		driveON.scene.longInfo.SetValue(fmt.Sprintf("%.5f", t.Lon))
		driveON.scene.speedInfo.SetValue(fmt.Sprintf("%.0f km/h", t.Speed))
	}
}

func (driveON *DriveOn) updateScene() {
	// Animações que não dependem de sensores continuam aqui
	// Se quiser misturar dados fakes com reais, faça a lógica aqui
}

func (driveON *DriveOn) destroy() {
	if driveON.render != nil {
		driveON.render.Destroy()
	}

}
