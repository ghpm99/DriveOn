package dtos

// TouchEvent representa o dado vindo do Tablet
type TouchEvent struct {
	X      float32
	Y      float32
	Action int // 0=Down, 1=Up, 2=Move
}

// FrameHeader é o cabeçalho binário que enviamos antes da imagem
type FrameHeader struct {
	Magic    uint32 // 0xDEADBEEF
	Width    uint32
	Height   uint32
	DataSize uint32
}

type TelemetryData struct {
	AccX, AccY, AccZ float64
	Light            float64
	Lat, Lon         float64
	Speed            float64
	HasGPS           bool
}
