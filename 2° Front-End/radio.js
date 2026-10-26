// Componente Global de Radio Flotante para Akiba Store
document.addEventListener("DOMContentLoaded", function() {
    // 1. Definir las páginas donde NO debe aparecer la radio
    const paginasExcluidas = ["/login.html", "/registro.html", "/perfil.html"]; // Puedes añadir perfil.html si prefieres
    const rutaActual = window.location.pathname;

    // Verificar si la página actual está en la lista de exclusión
    const estaExcluida = paginasExcluidas.some(exclusion => rutaActual.endsWith(exclusion));
    if (estaExcluida) {
        return; // Salir del script si es login, registro o perfil
    }

    // 2. Inyectar los estilos CSS de la radio dinámicamente (Corregidos y más robustos)
    const estiloRadio = document.createElement("style");
    estiloRadio.innerHTML = `
        #radioWidget {
            position: fixed;
            bottom: 30px !important;
            right: 30px !important;
            z-index: 999999 !important; /* Asegurar que esté sobre todo */
            pointer-events: auto !important;
        }
        .radio-panel {
            display: none;
            width: 320px !important;
            background: rgba(18, 18, 18, 0.98) !important;
            backdrop-filter: blur(15px) !important;
            -webkit-backdrop-filter: blur(15px) !important;
            border: 1px solid rgba(220, 53, 69, 0.5) !important;
            border-radius: 16px !important;
            box-shadow: 0 10px 40px rgba(0,0,0,0.7) !important;
            overflow: hidden !important;
            animation: slideInUp 0.35s ease-out !important;
            margin-bottom: 15px !important;
        }
        .radio-panel.active {
            display: block !important;
        }
        @keyframes slideInUp {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        #btnAbrirRadio {
            width: 65px !important;
            height: 65px !important;
            border-radius: 50% !important;
            box-shadow: 0 5px 20px rgba(220, 53, 69, 0.5) !important;
            transition: transform 0.3s ease, box-shadow 0.3s ease !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            margin-left: auto !important; /* Alinear a la derecha dentro del widget */
        }
        #btnAbrirRadio:hover {
            transform: scale(1.05) !important;
            box-shadow: 0 8px 25px rgba(220, 53, 69, 0.8) !important;
        }
    `;
    document.head.appendChild(estiloRadio);

    // 3. Inyectar el HTML del widget de radio en el body
    const widgetDiv = document.createElement("div");
    widgetDiv.id = "radioWidget";
    widgetDiv.innerHTML = `
        <div id="radioPanel" class="radio-panel p-4">
            <div class="d-flex justify-content-between align-items-center mb-3 border-bottom border-secondary pb-3">
                <span class="fw-bold text-danger fs-6"><i class="bi bi-broadcast me-2"></i> AKIBA RADIO LIVE</span>
                <button type="button" class="btn-close btn-close-white" id="cerrarPanelRadio" aria-label="Close"></button>
            </div>
            
            <div class="text-center mb-4 bg-black p-2 rounded border border-secondary">
                <span id="radioStatus" class="badge bg-secondary mb-2 px-3 py-1 text-uppercase small">Radio en pausa</span>
                <p id="radioStationName" class="text-light mb-0 fw-bold fs-5 text-truncate">Selecciona una emisora</p>
            </div>

            <div class="mb-4">
                <label for="selectEstacion" class="form-label text-white-50 small mb-1">Elige tu emisora:</label>
                <select id="selectEstacion" class="form-select bg-dark text-light border-secondary shadow-none">
                    <option value="https://icecast.omroep.nl/radio2-bb-mp3">Rock Clásico (Stream Live)</option>
                    <option value="https://n08.radiojar.com/8s5u5tpw0k8uv">Anime & J-Rock Hits (Stream Activo)</option>
                    <option value="https://stream.zeno.fm/f3wvbbqmdg8uv">Lo-Fi & Chill Beats</option>
                </select>
            </div>

            <audio id="audioPlayer" preload="none"></audio>

            <div class="d-flex justify-content-center align-items-center gap-2">
                <button id="btnPlayPause" class="btn btn-danger w-100 py-2 fw-bold shadow">
                    <i class="bi bi-play-fill fs-4 me-1"></i> Reproducir
                </button>
            </div>
        </div>

        <button id="btnAbrirRadio" class="btn btn-danger shadow-lg" title="Abrir Radio Akiba">
            <i class="bi bi-music-note-beamed fs-1 text-white"></i>
        </button>
    `;
    document.body.appendChild(widgetDiv);

    // 4. Lógica del reproductor y eventos
    const audioPlayer = document.getElementById("audioPlayer");
    const btnPlayPause = document.getElementById("btnPlayPause");
    const radioStatus = document.getElementById("radioStatus");
    const radioStationName = document.getElementById("radioStationName");
    const selectEstacion = document.getElementById("selectEstacion");
    const radioPanel = document.getElementById("radioPanel");
    const btnAbrirRadio = document.getElementById("btnAbrirRadio");
    const cerrarPanelRadio = document.getElementById("cerrarPanelRadio");

    let reproduciendo = localStorage.getItem("akiba_radio_playing") === "true";
    let estacionActual = localStorage.getItem("akiba_radio_url") || "https://icecast.omroep.nl/radio2-bb-mp3";

    // Funciones de UI
    function actualizarUIActivo() {
        const nombreEstacionText = selectEstacion.options[selectEstacion.selectedIndex].text;
        btnPlayPause.innerHTML = `<i class="bi bi-pause-fill fs-4 me-1"></i> Pausar`;
        btnPlayPause.className = "btn btn-outline-danger w-100 py-2 fw-bold shadow-none";
        radioStatus.className = "badge bg-success mb-2 px-3 py-1 text-uppercase small";
        radioStatus.textContent = "EN VIVO";
        radioStationName.textContent = nombreEstacionText;
    }

    function actualizarUIPausa() {
        btnPlayPause.innerHTML = `<i class="bi bi-play-fill fs-4 me-1"></i> Reproducir`;
        btnPlayPause.className = "btn btn-danger w-100 py-2 fw-bold shadow";
        radioStatus.className = "badge bg-secondary mb-2 px-3 py-1 text-uppercase small";
        radioStatus.textContent = "Radio en pausa";
        radioStationName.textContent = "Selecciona una emisora";
    }

    // Event Listeners
    btnAbrirRadio.addEventListener("click", () => radioPanel.classList.toggle("active"));
    cerrarPanelRadio.addEventListener("click", () => radioPanel.classList.remove("active"));

    // Inicializar estado
    selectEstacion.value = estacionActual;
    if (reproduciendo) {
        audioPlayer.src = estacionActual;
        audioPlayer.play().then(() => {
            actualizarUIActivo();
        }).catch(() => {
            reproduciendo = false;
            localStorage.setItem("akiba_radio_playing", "false");
            actualizarUIPausa();
        });
    }

    // Control de reproducción
    btnPlayPause.addEventListener("click", function() {
        if (!reproduciendo) {
            estacionActual = selectEstacion.value;
            audioPlayer.src = estacionActual;
            audioPlayer.play().then(() => {
                reproduciendo = true;
                localStorage.setItem("akiba_radio_playing", "true");
                localStorage.setItem("akiba_radio_url", estacionActual);
                actualizarUIActivo();
            }).catch(err => {
                alert("No se pudo conectar a la emisora en este momento. Intenta con otra.");
                console.error(err);
                reproduciendo = false;
                actualizarUIPausa();
            });
        } else {
            audioPlayer.pause();
            audioPlayer.src = ""; // Detener stream completamente
            reproduciendo = false;
            localStorage.setItem("akiba_radio_playing", "false");
            actualizarUIPausa();
        }
    });

    // Cambiar de estación en caliente
    selectEstacion.addEventListener("change", function() {
        if (reproduciendo) {
            estacionActual = selectEstacion.value;
            audioPlayer.src = estacionActual;
            audioPlayer.play();
            localStorage.setItem("akiba_radio_url", estacionActual);
            actualizarUIActivo();
        }
    });
});