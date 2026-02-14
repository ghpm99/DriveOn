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
	Type    string // "FAST" ou "SLOW"
	AccX    float64
	AccY    float64
	AccZ    float64
	MagX    float64
	MagY    float64
	MagZ    float64
	Light   float64
	Battery int
	Lat     float64
	Lon     float64
	Speed   float64
	FPS     int
	HasGPS  bool
}
