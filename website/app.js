const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add("visible");
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.12 });
document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));

const downloadLinks = document.querySelectorAll('a[href*="grabi-v0.3.1.apk"]');
downloadLinks.forEach((link) => {
  link.addEventListener("click", () => {
    link.classList.add("clicked");
    window.setTimeout(() => link.classList.remove("clicked"), 500);
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
