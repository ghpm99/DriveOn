package dtos

import "image"

type Frame struct {
	Width, Height, FrameSize int32
	Data                     []byte
	Image                    image.Image
}
