const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add("visible");
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.12 });
document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));

const downloadLinks = document.querySelectorAll("[data-download-link]");
downloadLinks.forEach((link) => {
  link.addEventListener("click", () => {
    link.classList.add("clicked");
    window.setTimeout(() => link.classList.remove("clicked"), 500);
  });

  const latestReleaseApi = "https://api.github.com/repos/raicore-official/grabi/releases/latest";
  fetch(latestReleaseApi, { headers: { Accept: "application/vnd.github+json" } })
    .then((response) => response.ok ? response.json() : Promise.reject(new Error("release lookup failed")))
    .then((release) => {
      const apk = release.assets?.find((asset) => asset.name.toLowerCase().endsWith(".apk"));
      if (!apk?.browser_download_url) return;
      document.querySelectorAll("[data-download-link]").forEach((link) => {
        link.href = apk.browser_download_url;
      });
      document.querySelectorAll("[data-release-version]").forEach((label) => {
        label.textContent = release.tag_name;
      });
      const downloads = release.assets.reduce((total, asset) => total + (asset.download_count || 0), 0);
      document.querySelectorAll("[data-download-count]").forEach((label) => {
        label.textContent = `${downloads.toLocaleString()} free downloads · ${release.tag_name}`;
      });
    })
    .catch(() => {
      document.querySelectorAll("[data-download-count]").forEach((label) => {
        label.textContent = "Free Android download · open source";
      });
    });

  const playAction = document.querySelector(".play-action");
  playAction?.addEventListener("click", () => {
    const isPlaying = playAction.getAttribute("aria-pressed") !== "true";
    playAction.setAttribute("aria-pressed", String(isPlaying));
    playAction.setAttribute("aria-label", isPlaying ? "Pause workflow" : "Play workflow");
    playAction.textContent = isPlaying ? "Ⅱ" : "▶";
  });
});

const screensCarousel = document.querySelector(".screens-carousel");
const screensScroll = screensCarousel?.querySelector(".screens-scroll");
const carouselButtons = screensCarousel?.querySelectorAll(".carousel-arrow");

if (screensScroll && carouselButtons?.length === 2) {
  const [previousButton, nextButton] = carouselButtons;
  const updateCarouselButtons = () => {
    const maxScroll = screensScroll.scrollWidth - screensScroll.clientWidth;
    previousButton.disabled = screensScroll.scrollLeft <= 2;
    nextButton.disabled = screensScroll.scrollLeft >= maxScroll - 2;
  };

  const moveCarousel = (direction) => {
    const card = screensScroll.querySelector(".screen-shot");
    const gap = Number.parseFloat(getComputedStyle(screensScroll).gap) || 0;
    screensScroll.scrollBy({
      left: direction * ((card?.getBoundingClientRect().width || screensScroll.clientWidth) + gap),
      behavior: "smooth",
    });
  };

  previousButton.addEventListener("click", () => moveCarousel(-1));
  nextButton.addEventListener("click", () => moveCarousel(1));
  screensScroll.addEventListener("scroll", updateCarouselButtons, { passive: true });
  screensScroll.addEventListener("keydown", (event) => {
    if (event.key === "ArrowLeft") moveCarousel(-1);
    if (event.key === "ArrowRight") moveCarousel(1);
  });
  window.addEventListener("resize", updateCarouselButtons);
  updateCarouselButtons();
}

const vpnStatus = document.querySelector("[data-vpn-status]");
const vpnToggle = vpnStatus?.querySelector(".vpn-toggle");
if (vpnStatus && vpnToggle) {
  vpnToggle.addEventListener("click", () => {
    const isOn = vpnStatus.dataset.vpnStatus !== "on";
    vpnStatus.dataset.vpnStatus = isOn ? "on" : "off";
    vpnToggle.setAttribute("aria-pressed", String(isOn));
    vpnToggle.setAttribute("aria-label", isOn ? "Turn VPN off" : "Turn VPN on");
    vpnStatus.querySelector(".vpn-status-title").textContent = isOn ? "Protected connection" : "VPN is off";
    vpnStatus.querySelector(".vpn-status-detail").textContent = isOn ? "Grabi VPN · WireGuard verified" : "Traffic is not protected";
    vpnStatus.querySelector(".vpn-status-state").textContent = isOn ? "ON" : "OFF";
  });
}

const musicPlay = document.querySelector(".music-play");
musicPlay?.addEventListener("click", () => {
  const isPlaying = musicPlay.getAttribute("aria-pressed") !== "true";
  musicPlay.setAttribute("aria-pressed", String(isPlaying));
  musicPlay.setAttribute("aria-label", isPlaying ? "Pause music" : "Play music");
  musicPlay.textContent = isPlaying ? "Ⅱ" : "▶";
  document.querySelector(".music-live")?.classList.toggle("paused", !isPlaying);
});
