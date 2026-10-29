export type TranscriptionState = 'idle' | 'recording' | 'transcribing';

export type TranscriptionErrorCode =
  | 'unsupported'
  | 'microphone-denied'
  | 'microphone-unavailable'
  | 'no-speech'
  | 'transcription-failed'
  | 'recording-failed';

export interface TranscriptionHandlers {
  onTranscript: (transcript: string) => void;
  onStateChange: (state: TranscriptionState) => void;
  onComplete: (transcript: string) => void;
  onError: (code: TranscriptionErrorCode) => void;
}

export interface SpeechTranscriptionService {
  isSupported(): boolean;
  start(handlers: TranscriptionHandlers): void;
  stop(): void;
  abort(): void;
}

interface SpeechAlternativeLike {
  transcript: string;
}

interface SpeechResultLike {
  isFinal: boolean;
  0: SpeechAlternativeLike;
}

interface SpeechResultEventLike {
  resultIndex: number;
  results: ArrayLike<SpeechResultLike>;
}

interface SpeechErrorEventLike {
  error?: string;
}

interface BrowserRecognitionLike {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onstart: (() => void) | null;
  onresult: ((event: SpeechResultEventLike) => void) | null;
  onerror: ((event: SpeechErrorEventLike) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

interface SpeechWindow extends Window {
  SpeechRecognition?: new () => BrowserRecognitionLike;
  webkitSpeechRecognition?: new () => BrowserRecognitionLike;
}

export class BrowserSpeechTranscriptionService implements SpeechTranscriptionService {
  private recognition: BrowserRecognitionLike | null = null;
  private handlers: TranscriptionHandlers | null = null;
  private finalTranscript = '';
  private stopping = false;

  isSupported(): boolean {
    if (typeof window === 'undefined') return false;
    const speechWindow = window as SpeechWindow;
    return Boolean(speechWindow.SpeechRecognition || speechWindow.webkitSpeechRecognition);
  }

  start(handlers: TranscriptionHandlers): void {
    this.abort();
    const speechWindow = window as SpeechWindow;
    const Recognition = speechWindow.SpeechRecognition || speechWindow.webkitSpeechRecognition;
    if (!Recognition) {
      handlers.onError('unsupported');
      return;
    }

    this.handlers = handlers;
    this.finalTranscript = '';
    this.stopping = false;
    const recognition = new Recognition();
    this.recognition = recognition;
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = navigator.language || 'en-US';
    recognition.onstart = () => handlers.onStateChange('recording');
    recognition.onresult = (event) => {
      let interimTranscript = '';
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const result = event.results[index];
        const text = result[0]?.transcript?.trim();
        if (!text) continue;
        if (result.isFinal) this.finalTranscript = `${this.finalTranscript} ${text}`.trim();
        else interimTranscript = `${interimTranscript} ${text}`.trim();
      }
      handlers.onTranscript(`${this.finalTranscript} ${interimTranscript}`.trim());
    };
    recognition.onerror = (event) => {
      this.stopping = false;
      handlers.onStateChange('idle');
      handlers.onError(this.mapError(event.error));
    };
    recognition.onend = () => {
      const transcript = this.finalTranscript.trim();
      handlers.onStateChange('idle');
      if (this.stopping) handlers.onComplete(transcript);
      this.recognition = null;
      this.handlers = null;
      this.stopping = false;
    };

    try {
      recognition.start();
    } catch (error) {
      this.recognition = null;
      this.handlers = null;
      handlers.onStateChange('idle');
      handlers.onError(this.mapError(error instanceof DOMException ? error.name : undefined));
    }
  }

  stop(): void {
    if (!this.recognition) return;
    this.stopping = true;
    this.handlers?.onStateChange('transcribing');
    try {
      this.recognition.stop();
    } catch {
      this.handlers?.onStateChange('idle');
      this.handlers?.onError('recording-failed');
    }
  }

  abort(): void {
    const recognition = this.recognition;
    this.recognition = null;
    this.handlers = null;
    this.stopping = false;
    this.finalTranscript = '';
    try {
      recognition?.abort();
    } catch {
      // Ignore teardown errors; typed answers remain available.
    }
  }

  private mapError(error?: string): TranscriptionErrorCode {
    if (error === 'not-allowed' || error === 'service-not-allowed' || error === 'NotAllowedError') {
      return 'microphone-denied';
    }
    if (error === 'audio-capture' || error === 'NotFoundError' || error === 'NotReadableError') {
      return 'microphone-unavailable';
    }
    if (error === 'no-speech') return 'no-speech';
    if (error === 'network') return 'transcription-failed';
    return 'recording-failed';
  }
}
