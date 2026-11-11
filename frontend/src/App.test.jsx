import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { App } from './main.jsx';

vi.mock('./api', () => ({ api: { routes: vi.fn().mockResolvedValue([]), logs: vi.fn().mockResolvedValue([]) } }));

describe('WiseWay shell', () => {
  it('shows the overview and honest demo disclosure', async () => {
    render(<App />);
    expect(screen.getAllByText('Make the next journey easier.').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Illustrative data · not live transit/).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('Your regular journeys belong here.')).length).toBeGreaterThan(0);
  });
});
