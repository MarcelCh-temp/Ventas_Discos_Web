import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';

type DialogTone = 'primary' | 'warning' | 'danger';

type DialogOptions = string | {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  tone?: DialogTone;
};

type DialogState = {
  kind: 'confirm' | 'alert';
  title: string;
  message: string;
  confirmText: string;
  cancelText: string;
  tone: DialogTone;
};

type DialogAPI = {
  confirm: (options: DialogOptions) => Promise<boolean>;
  alert: (options: DialogOptions) => Promise<void>;
};

const DialogContext = createContext<DialogAPI | null>(null);

export function useAppDialog(): DialogAPI {
  const context = useContext(DialogContext);
  if (!context) {
    throw new Error('useAppDialog debe usarse dentro de DialogProvider.');
  }
  return context;
}

export function DialogProvider({ children }: { children: ReactNode }) {
  const [dialog, setDialog] = useState<DialogState | null>(null);
  const resolveRef = useRef<((confirmed: boolean) => void) | null>(null);
  const cancelRef = useRef<HTMLButtonElement | null>(null);
  const confirmRef = useRef<HTMLButtonElement | null>(null);

  const open = useCallback((kind: 'confirm' | 'alert', options: DialogOptions) => {
    if (resolveRef.current) return Promise.resolve(false);

    const props: Exclude<DialogOptions, string> = typeof options === 'string'
      ? { message: options } : options;

    return new Promise<boolean>(resolve => {
      resolveRef.current = resolve;
      setDialog({
        kind,
        title: props.title || (kind === 'confirm' ? 'Confirmar operación' : 'Aviso'),
        message: props.message,
        confirmText: props.confirmText || (kind === 'confirm' ? 'Confirmar' : 'Entendido'),
        cancelText: props.cancelText || 'Cancelar',
        tone: props.tone || (kind === 'confirm' ? 'primary' : 'warning'),
      });
    });
  }, []);

  const confirm = useCallback((options: DialogOptions) => open('confirm', options), [open]);
  const alert = useCallback(async (options: DialogOptions) => { await open('alert', options); }, [open]);

  const close = useCallback((confirmed: boolean) => {
    const resolver = resolveRef.current;
    resolveRef.current = null;
    setDialog(null);
    resolver?.(confirmed);
  }, []);

  useEffect(() => {
    if (!dialog) return;
    const previouslyFocused = document.activeElement instanceof HTMLElement
      ? document.activeElement : null;
    const focusedButton = dialog.kind === 'confirm' ? cancelRef.current : confirmRef.current;
    focusedButton?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        close(false);
      }
      if (event.key === 'Tab') {
        const buttons = [cancelRef.current, confirmRef.current]
          .filter((button): button is HTMLButtonElement => Boolean(button));
        if (buttons.length === 0) return;
        const first = buttons[0];
        const last = buttons[buttons.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previouslyFocused?.focus();
    };
  }, [dialog, close]);

  useEffect(() => () => {
    resolveRef.current?.(false);
    resolveRef.current = null;
  }, []);

  return (
    <DialogContext.Provider value={{ confirm, alert }}>
      {children}
      {dialog && (
        <div
          className="nackson-dialog-overlay"
          onMouseDown={event => {
            if (event.target === event.currentTarget) close(false);
          }}
        >
          <div
            className="nackson-dialog-panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="nackson-dialog-title"
            aria-describedby="nackson-dialog-message"
          >
            <div className="nackson-dialog-header">
              <span className={`nackson-dialog-icon nackson-dialog-${dialog.tone}`} aria-hidden="true">
                <i className={dialog.tone === 'danger' ? 'bi bi-exclamation-triangle-fill' : 'bi bi-shield-check'} />
              </span>
              <div>
                <p className="nackson-dialog-eyebrow">ÑACKSON STORE</p>
                <h2 id="nackson-dialog-title" className="nackson-dialog-title">{dialog.title}</h2>
              </div>
            </div>
            <p id="nackson-dialog-message" className="nackson-dialog-message">{dialog.message}</p>
            <div className="nackson-dialog-actions">
              {dialog.kind === 'confirm' && (
                <button
                  ref={cancelRef}
                  type="button"
                  className="nackson-dialog-button nackson-dialog-cancel"
                  onClick={() => close(false)}
                >
                  {dialog.cancelText}
                </button>
              )}
              <button
                ref={confirmRef}
                type="button"
                className={`nackson-dialog-button nackson-dialog-accept nackson-dialog-accept-${dialog.tone}`}
                onClick={() => close(true)}
              >
                {dialog.confirmText}
              </button>
            </div>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  );
}
