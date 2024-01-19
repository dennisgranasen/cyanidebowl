import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

const rootElement = document.getElementById('warp-scores');
createRoot(rootElement).render(<App />);
