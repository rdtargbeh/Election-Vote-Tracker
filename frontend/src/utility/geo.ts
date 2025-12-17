// src/utils/geo.ts
// Small helper to capture browser geolocation with a timeout and graceful fallback.
// Usage: import { captureGeolocation } from '../utils/geo'; then `const coords = await captureGeolocation();`
export async function captureGeolocation(
  timeout = 8000
): Promise<{ latitude: number; longitude: number } | null> {
  return new Promise((resolve) => {
    if (!("geolocation" in navigator)) return resolve(null);

    const onSuccess = (pos: GeolocationPosition) => {
      resolve({
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
      });
    };

    const onError = () => resolve(null);

    navigator.geolocation.getCurrentPosition(onSuccess, onError, {
      enableHighAccuracy: true,
      maximumAge: 0,
      timeout,
    });
  });
}
