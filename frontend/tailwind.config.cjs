// tailwind,config,cjs

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "./src/shared/components/**/*.{js,ts,jsx,tsx}",
    "./src/pages/**/*.{js,ts,jsx,tsx}",
  ], // Ensure all relevant files are included
  theme: {
    extend: {
      colors: {
        primary: "#1D4ED8", // Define custom primary color
        neutral: "#F3F4F6", // Neutral background
      },
      borderRadius: {
        xl: "1rem", // Custom rounded radius
      },
    },
  },
  plugins: [],
};

// /** @type {import('tailwindcss').Config} */
// module.exports = {
//   content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
//   theme: {
//     extend: {},
//   },
//   plugins: [],
// };
