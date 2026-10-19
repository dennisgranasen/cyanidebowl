jest.mock('axios', () => ({ defaults: {}, get: jest.fn(), put: jest.fn() }));
jest.mock('./config', () => ({ isProduction: true, auth0Audience: 'test' }));

import axios from 'axios';
import AiReporterApi from './AiReporterApi';

beforeEach(() => {
  jest.useFakeTimers();
  axios.get.mockReset();
  axios.put.mockReset();
});
afterEach(() => jest.useRealTimers());

test('bounds a stalled HTTP request and aborts it', async () => {
  axios.get.mockImplementation(() => new Promise(() => {}));
  const result = AiReporterApi.autonomousWorkOverview(async () => 'token', jest.fn());
  const check = expect(result).rejects.toThrow('timed out');
  await jest.advanceTimersByTimeAsync(20000);
  await check;
  expect(axios.get.mock.calls[0][1].signal.aborted).toBe(true);
  expect(jest.getTimerCount()).toBe(0);
});

test('bounds token acquisition and never sends an expired request', async () => {
  let resolveToken;
  const result = AiReporterApi.autonomousWorkOverview(
    () => new Promise(resolve => { resolveToken = resolve; }), jest.fn()
  );
  const check = expect(result).rejects.toThrow('timed out');
  await jest.advanceTimersByTimeAsync(20000);
  await check;
  resolveToken('late-token');
  await jest.advanceTimersByTimeAsync(0);
  expect(axios.get).not.toHaveBeenCalled();
});

test('returns the overview and clears the deadline', async () => {
  const data = { autonomousExecutionEnabled: true, queue: {} };
  axios.get.mockResolvedValue({ data });
  await expect(AiReporterApi.autonomousWorkOverview(async () => 'token', jest.fn())).resolves.toEqual(data);
  expect(jest.getTimerCount()).toBe(0);
});

test.each([null, '', '<html></html>'])('rejects invalid overview %p', async data => {
  axios.get.mockResolvedValue({ data });
  await expect(AiReporterApi.autonomousWorkOverview(async () => 'token', jest.fn())).rejects.toThrow('invalid server response');
  expect(jest.getTimerCount()).toBe(0);
});

test('submits external provider output to the selected provider-call job', async () => {
  const data = { succeeded: true };
  axios.put.mockResolvedValue({ data });

  await expect(AiReporterApi.submitExternalAiResult(
    'quota group/one', 'job id', '{"answer":"ok"}', async () => 'token', jest.fn()
  )).resolves.toEqual(data);

  expect(axios.put).toHaveBeenCalledWith(
    '/admin/ai-autonomous-work/execution-queues/quota%20group%2Fone/jobs/job%20id/external-result',
    { content: '{"answer":"ok"}' },
    expect.objectContaining({ headers: { Authorization: 'Bearer token' } })
  );
});
