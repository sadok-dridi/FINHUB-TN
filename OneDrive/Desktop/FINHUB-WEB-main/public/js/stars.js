function initStars() {
    // Check if stars already exist (so we don't recreate them on Turbo navigation)
    if (document.getElementById('global-stars-injected')) return;

    // Create the container outside the body so Turbo never detaches it
    const starContainer = document.createElement('div');
    starContainer.id = 'global-stars-injected';
    starContainer.classList.add('star-container');
    // Ensure it covers the screen and sits at the very back
    starContainer.style.cssText = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: -9999; background: #0F0E17; pointer-events: none; overflow: hidden;';

    // Append to <html> directly (document.documentElement)
    // Turbo only replaces <body>, so this keeps animations 100% continuous
    document.documentElement.insertBefore(starContainer, document.body);

    const starCount = 150;

    for (let i = 0; i < starCount; i++) {
        const star = document.createElement('div');
        star.classList.add('star');

        // Random Position
        const x = Math.random() * 100;
        const y = Math.random() * 100;

        // Random Animation Properties
        const duration = 2 + Math.random() * 3 + 's';
        const delay = Math.random() * 5 + 's';
        const size = Math.random() * 2 + 'px'; // Varied sizes

        star.style.left = x + '%';
        star.style.top = y + '%';
        star.style.setProperty('--duration', duration);
        star.style.setProperty('--delay', delay);
        star.style.width = size;
        star.style.height = size;

        starContainer.appendChild(star);
    }

    // Add shooting stars at random, rare intervals
    function spawnRandomShootingStar() {
        createShootingStar(starContainer);

        // Spawn the next one randomly between 5 and 16 seconds from now
        const nextSpawnDelay = 5000 + Math.random() * 11000;
        setTimeout(spawnRandomShootingStar, nextSpawnDelay);
    }

    // Initial delay before the first shooting star (2 to 4 seconds)
    setTimeout(spawnRandomShootingStar, 2000 + Math.random() * 2000);
}

function createShootingStar(container) {
    const star = document.createElement('div');
    star.classList.add('shooting-star');

    // Randomize direction
    const isRtl = Math.random() > 0.5;

    if (isRtl) {
        star.classList.add('rtl');
        star.style.top = Math.random() * 50 + '%';
        star.style.left = (Math.random() * 50 + 50) + '%';
    } else {
        star.classList.add('ltr');
        star.style.top = Math.random() * 50 + '%';
        star.style.left = (Math.random() * 50) + '%';
    }

    container.appendChild(star);

    const timeout = isRtl ? 15000 : 20000;
    setTimeout(() => {
        star.remove();
    }, timeout);
}

// Support both standard loads and Turbo navigations
document.addEventListener('DOMContentLoaded', initStars);
document.addEventListener('turbo:load', initStars);


