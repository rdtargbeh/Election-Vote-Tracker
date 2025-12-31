// Example: call before submit; fill latitude / longitude in payload if user allows.
export function captureGeolocation(timeout = 8000) {
  return new Promise((resolve) => {
    if (!navigator.geolocation) return resolve(null);
    navigator.geolocation.getCurrentPosition(
      (pos) =>
        resolve({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        }),
      (err) => resolve(null),
      { enableHighAccuracy: true, maximumAge: 0, timeout }
    );
  });
}

// Example usage in form submit:
// const coords = await captureGeolocation();
// if (coords) payload.latitude = coords.latitude; payload.longitude = coords.longitude;
