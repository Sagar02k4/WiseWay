import { expect } from 'vitest';
import * as matchers from '@testing-library/jest-dom/matchers';
expect.extend(matchers);
document.body.innerHTML = '<div id="root"></div>';
