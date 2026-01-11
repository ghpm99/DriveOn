package render

import (
	"strconv"

	"github.com/veandco/go-sdl2/img"
)

func (r *Renderer) InitTexture() error {

	if err := img.Init(img.INIT_PNG); err != nil {
		panic(err)
	}
	defer img.Quit()

	texture, err := img.LoadTexture(r.renderer, r.getSceneURI())
	if err != nil {
		return err
	}
	r.backgroundTexture = texture
	return nil
}

func (r *Renderer) SetScene(scene int) {
	r.scene = scene
}

func (r *Renderer) GetScene() int {
	return r.scene
}

func (r *Renderer) getSceneURI() string {
	return "assets/backgrounds/scene" + strconv.Itoa(r.scene) + ".png"
}
