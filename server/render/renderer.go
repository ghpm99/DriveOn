package render

import (
	"driveon/dtos"
	"encoding/binary"
	"fmt"
	"image"
	"image/color"
	"time"
	"unsafe"

	"github.com/veandco/go-sdl2/sdl"
	"github.com/veandco/go-sdl2/ttf"
)

type TouchState struct {
	X, Y       int32
	IsActive   bool
	LastUpdate time.Time
}

type Renderer struct {
	window            *sdl.Window
	renderer          *sdl.Renderer
	font              *ttf.Font
	lastTime          time.Time
	frames            int
	fps               int
	backgroundTexture *sdl.Texture
	scene             int
	width, height     int32
	Infos             []*Info
	FrameBuffer       chan dtos.Frame
	pixelBuffer       []byte
	TouchCursor       chan dtos.TouchEvent

	// Estado do Cursor
	currentTouch TouchState
}

func New(width, height int32) (*Renderer, error) {
	// ... (Mesma inicialização do SDL2) ...
	if err := sdl.Init(sdl.INIT_VIDEO); err != nil {
		return nil, err
	}
	if err := ttf.Init(); err != nil {
		return nil, err
	}

	w, err := sdl.CreateWindow("DriveOn MVP", sdl.WINDOWPOS_CENTERED, sdl.WINDOWPOS_CENTERED, width, height, sdl.WINDOW_SHOWN)
	if err != nil {
		return nil, err
	}

	r, err := sdl.CreateRenderer(w, -1, sdl.RENDERER_ACCELERATED)
	if err != nil {
		return nil, err
	}

	font, err := ttf.OpenFont("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 18)
	if err != nil {
		return nil, err
	}

	return &Renderer{
		window:      w,
		renderer:    r,
		font:        font,
		lastTime:    time.Now(),
		scene:       1,
		width:       width,
		height:      height,
		FrameBuffer: make(chan dtos.Frame, 2),
		pixelBuffer: make([]byte, width*height*2),
		TouchCursor: make(chan dtos.TouchEvent, 50),
	}, nil
}

func (r *Renderer) Destroy() {
	r.font.Close()
	if r.backgroundTexture != nil {
		r.backgroundTexture.Destroy()
	}
	r.renderer.Destroy()
	r.window.Destroy()
	ttf.Quit()
	sdl.Quit()
}

func (r *Renderer) Draw() error {
	if r.backgroundTexture == nil {
		if err := r.InitTexture(); err != nil { // Assumindo que InitTexture já existe no seu código
			return err
		}
	}
	r.renderer.Clear()
	if r.backgroundTexture != nil {
		r.renderer.Copy(r.backgroundTexture, nil, nil)
	}

	if r.Infos != nil {
		r.drawInfos(r.Infos, 10, 80, 60) // Assumindo que drawInfos existe
	}

	r.updateFPS()
	r.drawText(10, 10, "PC FPS: "+itoa(r.fps))

	// LÓGICA DE CURSOR TOUCH NÃO-BLOQUEANTE
	r.processTouchCursor()

	r.renderer.Present()
	return nil
}

// processTouchCursor lê o canal de toques sem travar e desenha se estiver ativo
func (r *Renderer) processTouchCursor() {
	// 1. Lê a fila de eventos sem bloquear (O Pulo do Gato)
	for {
		select {
		case event := <-r.TouchCursor:
			r.currentTouch.X = int32(event.X)
			r.currentTouch.Y = int32(event.Y)

			if event.Action == 1 { // ACTION_UP (Dedo soltou)
				r.currentTouch.IsActive = false
			} else { // ACTION_DOWN ou ACTION_MOVE
				r.currentTouch.IsActive = true
				r.currentTouch.LastUpdate = time.Now()
			}
		default:
			goto DrawStep
		}
	}

DrawStep:
	if r.currentTouch.IsActive && time.Since(r.currentTouch.LastUpdate) > 200*time.Millisecond {
		r.currentTouch.IsActive = false
	}

	if r.currentTouch.IsActive {
		r.renderer.SetDrawColor(255, 0, 0, 255) // Vermelho
		rect := sdl.Rect{X: r.currentTouch.X - 5, Y: r.currentTouch.Y - 5, W: 10, H: 10}
		r.renderer.FillRect(&rect)
	}
}

func (r *Renderer) GetAsImage() error {
	w, h, _ := r.renderer.GetOutputSize()

	requiredSize := int(w * h * 2)
	if len(r.pixelBuffer) < requiredSize {
		r.pixelBuffer = make([]byte, requiredSize)
	}

	err := r.renderer.ReadPixels(nil, sdl.PIXELFORMAT_RGB565, unsafe.Pointer(&r.pixelBuffer[0]), int(w)*2)
	if err != nil {
		return err
	}

	img := image.NewRGBA(image.Rect(0, 0, int(r.width), int(r.height)))

	for y := 0; y < int(r.height); y++ {
		for x := 0; x < int(r.width); x++ {
			idx := (y*int(r.width) + x) * 2
			p := binary.LittleEndian.Uint16(r.pixelBuffer[idx:])

			r5 := uint8((p>>11)&0x1F) << 3
			g6 := uint8((p>>5)&0x3F) << 2
			b5 := uint8(p&0x1F) << 3

			img.SetRGBA(x, y, color.RGBA{r5, g6, b5, 255})
		}
	}
	r.FrameBuffer <- dtos.Frame{Image: img}
	return nil
}

func (r *Renderer) ReadScreen() error {

	w, h, _ := r.renderer.GetOutputSize()

	requiredSize := int(w * h * 2)
	if len(r.pixelBuffer) < requiredSize {
		r.pixelBuffer = make([]byte, requiredSize)
	}

	err := r.renderer.ReadPixels(nil, sdl.PIXELFORMAT_RGB565, unsafe.Pointer(&r.pixelBuffer[0]), int(w)*2)
	if err != nil {
		return err
	}

	dataToSend := make([]byte, requiredSize)
	copy(dataToSend, r.pixelBuffer)

	r.FrameBuffer <- dtos.Frame{Data: dataToSend, Width: w, Height: h, FrameSize: int32(requiredSize)}
	return nil
}

func (r *Renderer) updateFPS() {
	r.frames++
	if time.Since(r.lastTime) >= time.Second {
		r.fps = r.frames
		r.frames = 0
		r.lastTime = time.Now()
	}
}

func (r *Renderer) drawText(x, y int32, text string) error {

	color := sdl.Color{R: 200, G: 200, B: 200, A: 255}
	surface, err := r.font.RenderUTF8Blended(text, color)
	if err != nil {
		return err
	}
	defer surface.Free()

	texture, err := r.renderer.CreateTextureFromSurface(surface)
	if err != nil {
		return err
	}
	defer texture.Destroy()

	dst := sdl.Rect{X: x, Y: y, W: surface.W, H: surface.H}
	return r.renderer.Copy(texture, nil, &dst)
}

func itoa(v int) string { return fmt.Sprintf("%d", v) }

func (r *Renderer) SetInfos(infos []*Info) { r.Infos = infos }
