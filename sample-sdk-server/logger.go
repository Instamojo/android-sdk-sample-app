package main

import (
	"fmt"
	"net/http"
	"time"
)

//LoggingHandler wraps the handler with logger
func LoggingHandler(handler http.Handler) http.Handler {
	return loggingHandler{handler}
}

type loggingHandler struct {
	handler http.Handler
}

func (l loggingHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	writer := &responseWriter{w, 0, 0}
	l.handler.ServeHTTP(writer, r)
	end := time.Now()
	latency := end.Sub(start)
	fmt.Printf("%s [%v] \"%s %s %s\" %d %d \"%s\" %v\n",
		r.RemoteAddr, end.Format(time.RFC1123Z),
		r.Method, r.URL.Path, r.Proto,
		writer.status, writer.size, r.Header.Get("User-Agent"), latency)
}

type responseWriter struct {
	W      http.ResponseWriter
	status int
	size   int
}

func (w *responseWriter) Header() http.Header {
	return w.W.Header()
}

func (w *responseWriter) Write(data []byte) (int, error) {
	if w.status == 0 {
		w.status = http.StatusOK
	}
	size, err := w.W.Write(data)
	w.size += size
	return size, err
}

func (w *responseWriter) WriteHeader(header int) {
	w.W.WriteHeader(header)
	w.status = header
}
