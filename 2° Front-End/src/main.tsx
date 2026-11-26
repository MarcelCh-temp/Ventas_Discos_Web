import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { NacksonThemeProvider } from './components/TemaNackson';
import './styles/nackson-tema.css';
import './styles/nackson-tema-v2.css';
import { DialogProvider } from './components/ConfirmDialog';
import './styles/confirm-dialog.css';
import './styles/base.css';
import './styles/filtros-categorias.css';
import './styles/nackson-identidad.css';
import './styles/nackson-home.css';
import './styles/nackson-global-v3.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><DialogProvider><NacksonThemeProvider><App /></NacksonThemeProvider></DialogProvider></React.StrictMode>
);
