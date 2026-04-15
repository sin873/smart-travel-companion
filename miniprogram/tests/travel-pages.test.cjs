const test = require('node:test')
const assert = require('node:assert/strict')

function loadPageModule(relativePath, options = {}) {
  const resolvedPath = require.resolve(relativePath)
  delete require.cache[resolvedPath]

  let pageConfig = null
  global.Page = config => {
    pageConfig = config
  }
  global.getApp = () => options.app || { globalData: {} }

  require(resolvedPath)

  if (!pageConfig) {
    throw new Error(`Failed to load page config from ${relativePath}`)
  }

  return pageConfig
}

function createPageInstance(pageConfig, initialData = {}) {
  const instance = {
    data: {
      ...JSON.parse(JSON.stringify(pageConfig.data || {})),
      ...initialData
    },
    setData(update) {
      Object.entries(update).forEach(([key, value]) => {
        const segments = key.split('.')
        let cursor = this.data

        for (let index = 0; index < segments.length - 1; index += 1) {
          const segment = segments[index]
          const nextSegment = segments[index + 1]

          if (cursor[segment] == null) {
            cursor[segment] = /^\d+$/.test(nextSegment) ? [] : {}
          }

          cursor = cursor[segment]
        }

        cursor[segments[segments.length - 1]] = value
      })
    }
  }

  Object.entries(pageConfig).forEach(([key, value]) => {
    if (typeof value === 'function') {
      instance[key] = value.bind(instance)
    }
  })

  return instance
}

test('plan page validates required fields and date ordering', () => {
  const pageConfig = loadPageModule('../pages/plan/plan.js')
  const page = createPageInstance(pageConfig)

  assert.equal(page.validateForm(), '请先补全必填信息')

  page.setData({
    'formData.destination': '杭州',
    'formData.startDate': '2026-05-03',
    'formData.endDate': '2026-05-01'
  })
  assert.equal(page.validateForm(), '结束日期不能早于开始日期')

  page.setData({
    'formData.endDate': '2026-05-05'
  })
  assert.equal(page.validateForm(), '')

  page.checkCanSubmit()
  assert.equal(page.data.canSubmit, true)
})

test('planning page derives steps and summary preview from task result', () => {
  const pageConfig = loadPageModule('../pages/planning/planning.js')
  const page = createPageInstance(pageConfig)

  assert.equal(page.getCurrentStep(10, 'PROCESSING'), 1)
  assert.equal(page.getCurrentStep(50, 'PROCESSING'), 2)
  assert.equal(page.getCurrentStep(80, 'PROCESSING'), 3)
  assert.equal(page.getCurrentStep(100, 'COMPLETED'), 4)

  page.updateStatus({
    status: 'COMPLETED',
    progress: 100,
    message: '行程规划完成！',
    itinerary: {
      title: '杭州 3 日游',
      travelerCount: 2,
      destination: '杭州',
      summary: '基于高德 POI 生成\n\n智能伴旅Agent建议:\n避开早高峰'
    }
  })

  assert.equal(page.data.statusTitle, '行程已生成')
  assert.equal(page.data.summaryPreview, '基于高德 POI 生成')
  assert.equal(page.data.dayCount, 0)
})

test('itinerary page parses summary and strips UI fields before save', () => {
  const pageConfig = loadPageModule('../pages/itinerary/itinerary.js')
  const page = createPageInstance(pageConfig, {
    itinerary: {
      itineraryId: 'itin_001',
      title: '杭州 2 日游',
      destination: '杭州',
      startDate: '2026-05-01',
      endDate: '2026-05-02',
      travelerCount: 2,
      budget: 1200,
      status: 'PLANNED',
      summary: '基础摘要\n\n智能伴旅Agent建议:\n知识库提示：西湖音乐喷泉夜间更佳\n路线提示：上午先灵隐寺后西湖',
      days: [
        {
          dayId: 'day_1',
          dayNumber: 1,
          travelDate: '2026-05-01',
          title: '经典游',
          notes: '路线提示：上午打车更稳妥',
          expanded: true,
          displayDate: '5月1日 周五',
          attractions: [
            {
              itemId: 'item_1',
              attractionId: 'poi_1',
              attractionName: '西湖',
              address: '杭州西湖',
              latitude: 30.25,
              longitude: 120.16,
              startTime: '09:00:00',
              endTime: '11:00:00',
              durationMinutes: 120,
              orderIndex: 0,
              notes: '建议上午游览',
              displayTime: '09:00 - 11:00'
            }
          ]
        }
      ]
    }
  })

  const parsed = page.parseSummary(page.data.itinerary.summary)
  assert.equal(parsed.overviewSummary, '基础摘要')
  assert.match(parsed.agentAdvice, /知识库提示/)
  assert.deepEqual(parsed.ragHints, ['知识库提示：西湖音乐喷泉夜间更佳'])

  const payload = page.buildSavePayload()
  assert.equal(payload.days[0].expanded, undefined)
  assert.equal(payload.days[0].displayDate, undefined)
  assert.equal(payload.days[0].items[0].displayTime, undefined)
  assert.equal(payload.days[0].items[0].attractionName, '西湖')
})

test('my-itineraries page formats status and summary text', () => {
  const pageConfig = loadPageModule('../pages/my-itineraries/my-itineraries.js', {
    app: {
      globalData: {
        userInfo: { nickName: 'demo' }
      }
    }
  })
  const page = createPageInstance(pageConfig)

  assert.equal(page.getStatusText('PLANNED'), '已规划')
  assert.equal(page.getStatusText('UNKNOWN'), 'UNKNOWN')
  assert.equal(page.getSummaryText('第一行\n第二行'), '第一行')

  page.syncUser()
  assert.deepEqual(page.data.userInfo, { nickName: 'demo' })
})
