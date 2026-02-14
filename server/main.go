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
	lightInfo,
	batteryInfo,
	androidFpsInfo *render.Info
}

// Guarda o estado atual para que o render do G-Force ou UI não pisque
var currentTelemetry dtos.TelemetryData

type DriveOn struct {
	render      *render.Renderer
	displaylink *displaylink.ConnectionManager
	running     bool
	scene       Scene
	isTouching  bool
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

	driveON.displaylink = displaylink.New("192.168.42.129", "9000") // Substitua pelo IP real
	go driveON.displaylink.Start()

	driveON.render = r

	speed := render.NewInfo("Speed", "0 km/h")
	accX := render.NewInfo("AccX", "0")
	accY := render.NewInfo("AccY", "0")
	accZ := render.NewInfo("AccZ", "0")
	lat := render.NewInfo("Lat", "0")
	lon := render.NewInfo("Lon", "0")
	light := render.NewInfo("Luz", "0")
	battery := render.NewInfo("Bateria", "0%")
	androidFps := render.NewInfo("Android FPS", "0")

	driveON.scene = Scene{
		speedInfo:      speed,
		lightInfo:      light,
		accXInfo:       accX,
		accYInfo:       accY,
		accZInfo:       accZ,
		latInfo:        lat,
		longInfo:       lon,
		batteryInfo:    battery,
		androidFpsInfo: androidFps,
	}

	r.SetInfos([]*render.Info{
		speed, light, battery, androidFps, accX, accY, accZ, lat, lon,
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
	ticker := time.NewTicker(time.Second / 60) // 60 FPS no PC
	defer ticker.Stop()

	for driveON.running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				driveON.running = false
			}
		}

		// 1. Drena rede (Touch e Sensores)
		driveON.processNetworkEvents()

		// 2. Lógica local e UI
		driveON.updateScene()

		// 3. Renderiza no PC (E processa a lógica de timeout do cursor de touch)
		if err := driveON.render.Draw(); err != nil {
			log.Println("Erro de render:", err)
		}

		// 4. Captura tela para enviar de volta a 60hz
		select {
		case <-ticker.C:
			err := driveON.render.ReadScreen()
			if err != nil {
				log.Println("Erro ao ler tela:", err)
			}
		default:
		}
	}
}

func (driveON *DriveOn) processNetworkEvents() {
	for {
		select {
		case touch := <-driveON.displaylink.TouchOutput:
			// Envia direto pro Renderer lidar com estado e timeout
			driveON.render.TouchCursor <- touch

		case telemetry := <-driveON.displaylink.SensorOutput:
			driveON.handleTelemetry(telemetry)

		default:
			return // Fila vazia, volta pro mainLoop
		}
	}
}

func (driveON *DriveOn) handleTelemetry(t dtos.TelemetryData) {
	// Mescla dados parciais (Fast ou Slow) no estado global
	if t.Type == "FAST" {
		currentTelemetry.AccX = t.AccX
		currentTelemetry.AccY = t.AccY
		currentTelemetry.AccZ = t.AccZ
		currentTelemetry.MagX = t.MagX
		currentTelemetry.MagY = t.MagY
		currentTelemetry.MagZ = t.MagZ
	} else if t.Type == "SLOW" {
		currentTelemetry.Light = t.Light
		currentTelemetry.Battery = t.Battery
		currentTelemetry.Lat = t.Lat
		currentTelemetry.Lon = t.Lon
		currentTelemetry.Speed = t.Speed
		currentTelemetry.FPS = t.FPS
		currentTelemetry.HasGPS = t.HasGPS
	}
}

func (driveON *DriveOn) updateScene() {
	// Atualiza os textos da tela com os dados combinados mais recentes
	driveON.scene.accXInfo.SetValue(fmt.Sprintf("%.2f", currentTelemetry.AccX))
	driveON.scene.accYInfo.SetValue(fmt.Sprintf("%.2f", currentTelemetry.AccY))
	driveON.scene.accZInfo.SetValue(fmt.Sprintf("%.2f", currentTelemetry.AccZ))

	driveON.scene.lightInfo.SetValue(fmt.Sprintf("%.0f lx", currentTelemetry.Light))
	driveON.scene.batteryInfo.SetValue(fmt.Sprintf("%d%%", currentTelemetry.Battery))
	driveON.scene.androidFpsInfo.SetValue(fmt.Sprintf("%d", currentTelemetry.FPS))

	if currentTelemetry.HasGPS {
		driveON.scene.latInfo.SetValue(fmt.Sprintf("%.5f", currentTelemetry.Lat))
		driveON.scene.longInfo.SetValue(fmt.Sprintf("%.5f", currentTelemetry.Lon))
		driveON.scene.speedInfo.SetValue(fmt.Sprintf("%.0f km/h", currentTelemetry.Speed))
	}
}

func (driveON *DriveOn) destroy() {
	if driveON.render != nil {
		driveON.render.Destroy()
	}
}
