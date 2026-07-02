const assert = require('assert')
const { pathToFileURL } = require('url')
const path = require('path')

async function main() {
  const modulePath = pathToFileURL(path.resolve(__dirname, '../services/ppgHeartRate.js')).href
  const {
    buildSyntheticPpgSamples,
    estimateHeartRate,
    extractRedMeanFromImageData
  } = await import(modulePath)

  const cases = [
    { bpm: 62, tolerance: 3 },
    { bpm: 78, tolerance: 3 },
    { bpm: 112, tolerance: 4 }
  ]

  for (const item of cases) {
    const samples = buildSyntheticPpgSamples({
      bpm: item.bpm,
      durationSeconds: 14,
      sampleRate: 30,
      noise: 0.35
    })
    const result = estimateHeartRate(samples)
    assert.ok(result.reliable, `Expected reliable result for ${item.bpm} bpm, got ${JSON.stringify(result)}`)
    assert.ok(
      Math.abs(result.bpm - item.bpm) <= item.tolerance,
      `Expected ${item.bpm} bpm, got ${result.bpm}`
    )
  }

  const weak = estimateHeartRate(buildSyntheticPpgSamples({
    bpm: 80,
    durationSeconds: 14,
    sampleRate: 30,
    amplitude: 0.01
  }))
  assert.strictEqual(weak.reliable, false, 'Weak signal should not be reliable')

  const width = 4
  const height = 4
  const data = new Uint8ClampedArray(width * height * 4)
  for (let i = 0; i < width * height; i += 1) {
    data[i * 4] = 120
    data[i * 4 + 1] = 20
    data[i * 4 + 2] = 10
    data[i * 4 + 3] = 255
  }
  assert.strictEqual(extractRedMeanFromImageData({ width, height, data }, { cropRatio: 1 }), 120)

  console.log('PPG heart-rate check passed: synthetic signals and red-channel extraction are stable.')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
