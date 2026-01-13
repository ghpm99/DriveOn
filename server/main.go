package main

import (
	"fmt"
	"log"
	"math/rand"
	"time"

	"driveon/render"

	"github.com/veandco/go-sdl2/sdl"
)

const (
	screenWidth  = 1024
	screenHeight = 600
)

type Scene struct {
	Infos []*render.Info
}

type SmoothValue struct {
	Value int
	Min   int
	Max   int
	Step  int
}

func main() {
	r, err := render.New(screenWidth, screenHeight)
	if err != nil {
		log.Fatal(err)
	}
	defer r.Destroy()

	speed := render.NewInfo("Speed", "120 km/h")
	rpm := render.NewInfo("RPM", "3000")
	fuel := render.NewInfo("Fuel", "50%")
	temp := render.NewInfo("Temp", "90°C")
	gear := render.NewInfo("Gear", "D")

	r.SetInfos([]*render.Info{
		speed,
		rpm,
		fuel,
		temp,
		gear,
	})

	running := true

	for running {
		for event := sdl.PollEvent(); event != nil; event = sdl.PollEvent() {
			switch event.(type) {
			case *sdl.QuitEvent:
				running = false
			}
		}
		updateInfos(speed, rpm, fuel, temp)

		if err := r.Draw(); err != nil {
			log.Println(err)
		}

		time.Sleep(time.Millisecond * 16) // ~60 FPS
	}
}

func updateInfos(
	speedInfo, rpmInfo, fuelInfo, tempInfo *render.Info,
) {
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

	speedInfo.SetValue(fmt.Sprintf("%d km/h", speed.Value))
	rpmInfo.SetValue(fmt.Sprintf("%d RPM", rpm.Value))
	fuelInfo.SetValue(fmt.Sprintf("%d%%", fuel.Value))
	tempInfo.SetValue(fmt.Sprintf("%d°C", temp.Value))
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
