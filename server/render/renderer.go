package render

import (
	"driveon/dtos"
	"fmt"
	"time"
	"unsafe"

	"github.com/veandco/go-sdl2/sdl"
	"github.com/veandco/go-sdl2/ttf"
)

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
}

func New(width, height int32) (*Renderer, error) {
	if err := sdl.Init(sdl.INIT_VIDEO); err != nil {
		return nil, err
	}

	if err := ttf.Init(); err != nil {
		return nil, err
	}

	w, err := sdl.CreateWindow(
		"DriveOn MVP",
		sdl.WINDOWPOS_CENTERED,
		sdl.WINDOWPOS_CENTERED,
		width,
		height,
		sdl.WINDOW_SHOWN,
	)
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
	r.backgroundTexture.Destroy()
	r.renderer.Destroy()
	r.window.Destroy()
	ttf.Quit()
	sdl.Quit()
}

func (r *Renderer) Draw() error {
	// fundo
	if r.backgroundTexture == nil {

		if err := r.InitTexture(); err != nil {
			return err
		}
	}
	r.renderer.Clear()
	r.renderer.Copy(r.backgroundTexture, nil, nil)

	if r.Infos != nil {
		r.drawInfos(
			r.Infos,
			10,
			80,
			60,
		)
	}

	r.updateFPS()
	r.drawText(10, 10, "FPS: "+itoa(r.fps))

	// r.DrawTouchCursor(<-r.TouchCursor)

	r.renderer.Present()
	return nil
}

func (r *Renderer) ReadScreen() error {
	w, h, _ := r.renderer.GetOutputSize()

	requiredSize := int(w * h * 2)
	if len(r.pixelBuffer) < requiredSize {
		r.pixelBuffer = make([]byte, requiredSize)
	}

	err := r.renderer.ReadPixels(
		nil,
		sdl.PIXELFORMAT_RGB565,
		unsafe.Pointer(&r.pixelBuffer[0]),
		int(w)*2,
	)

	if err != nil {
		return err
	}

	dataToSend := make([]byte, requiredSize)
	copy(dataToSend, r.pixelBuffer)

	r.FrameBuffer <- dtos.Frame{
		Data:      dataToSend,
		Width:     w,
		Height:    h,
		FrameSize: int32(requiredSize),
	}

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

	dst := sdl.Rect{
		X: x,
		Y: y,
		W: surface.W,
		H: surface.H,
	}

	return r.renderer.Copy(texture, nil, &dst)
}

func itoa(v int) string {
	return fmt.Sprintf("%d", v)
}

func (r *Renderer) SetInfos(infos []*Info) {
	r.Infos = infos
}

func (r *Renderer) DrawTouchCursor(touchEvent dtos.TouchEvent) {

	// Desenha uma bolinha vermelha onde o dedo está
	r.renderer.SetDrawColor(255, 0, 0, 255)
	// Desenho simples de um retângulo pequeno (ou círculo se implementar)
	rect := sdl.Rect{X: int32(touchEvent.X) - 5, Y: int32(touchEvent.Y) - 5, W: 10, H: 10}
	r.renderer.FillRect(&rect)

}
