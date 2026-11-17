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
