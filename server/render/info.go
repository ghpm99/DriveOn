package render

type Info struct {
	label string
	value string
}

func NewInfo(label, value string) *Info {
	return &Info{
		label: label,
		value: value,
	}
}

func (i *Info) SetValue(value string) {
	i.value = value
}

func (r *Renderer) drawInfos(infos []*Info, startX, startY, lineHeight int32) error {

	totalCards := len(infos)
	if totalCards == 0 {
		return nil
	}

	totalWidth := r.width - startX*2
	cardWidth := totalWidth / 5

	for i, info := range infos {
		y := startY + int32(i/5)*lineHeight
		x := startX + int32(i%5)*cardWidth
		r.drawInfo(info, x, y, lineHeight)
	}
	return nil
}

func (r *Renderer) drawInfo(info *Info, x, y, lineHeight int32) error {
	r.drawText(x, y, info.label)
	r.drawText(x, y+30, info.value)
	return nil

}
