function col(key, label, width, wrap = false) {
  return { key, label, width, wrap }
}

function field(key, label, type = 'text', extra = {}) {
  return { key, label, type, ...extra }
}

const yesNoOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const privacyOptions = [
  { label: '公开', value: 'public' },
  { label: '部分公开', value: 'partial' },
  { label: '私密', value: 'private' }
]

export const adminResourceConfigs = {
  users: {
    key: 'users',
    resource: 'users',
    title: '用户管理',
    eyebrow: '用户数据',
    description: '查看和维护用户账号、角色、状态与密码。',
    columns: [
      col('id', '编号', '90px'),
      col('username', '用户名', '130px'),
      col('phone', '手机号', '140px'),
      col('password', '加密密码', '260px'),
      col('nickname', '昵称', '130px'),
      col('role', '角色', '100px'),
      col('status', '状态', '90px'),
      col('created_at', '创建时间', '180px')
    ],
    fields: [
      field('username', '用户名'),
      field('password', '密码', 'text', { createOnly: true }),
      field('phone', '手机号'),
      field('nickname', '昵称'),
      field('gender', '性别', 'select', {
        options: [
          { label: '未知', value: 'unknown' },
          { label: '男', value: 'male' },
          { label: '女', value: 'female' }
        ]
      }),
      field('birthday', '生日'),
      field('avatar', '头像地址'),
      field('role', '角色', 'select', {
        options: [
          { label: '普通用户', value: 'USER' },
          { label: '管理员', value: 'ADMIN' }
        ]
      }),
      field('status', '状态', 'select', { options: yesNoOptions })
    ]
  },
  'diet-records': {
    key: 'diet-records',
    resource: 'diet-records',
    title: '饮食记录管理',
    eyebrow: '饮食数据',
    description: '按用户、餐次和食物查看全站饮食记录。',
    allowCreate: false,
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('phone', '手机号', '140px'),
      col('meal_type', '餐次', '100px'),
      col('food_name', '食物', '160px'),
      col('weight', '重量(g)', '110px'),
      col('calories', '热量(kcal)', '120px'),
      col('record_time', '记录时间', '180px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('meal_type', '餐次'),
      field('food_name', '食物名称'),
      field('weight', '重量(g)', 'number', { step: '0.01' }),
      field('calories', '热量(kcal)', 'number', { step: '0.01' }),
      field('record_time', '记录时间')
    ]
  },
  'health-records': {
    key: 'health-records',
    resource: 'health-records',
    title: '健康档案管理',
    eyebrow: '健康数据',
    description: '维护用户身高、体重、BMI 和 BMR 记录。',
    allowCreate: false,
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('phone', '手机号', '140px'),
      col('height', '身高(cm)', '110px'),
      col('weight', '体重(kg)', '110px'),
      col('bmi', 'BMI', '90px'),
      col('bmr', 'BMR', '100px'),
      col('record_time', '记录时间', '180px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('height', '身高(cm)', 'number', { step: '0.01' }),
      field('weight', '体重(kg)', 'number', { step: '0.01' }),
      field('bmi', 'BMI', 'number', { step: '0.01' }),
      field('bmr', 'BMR', 'number', { step: '0.01' }),
      field('record_time', '记录时间')
    ]
  },
  'heart-records': {
    key: 'heart-records',
    resource: 'heart-records',
    title: '心率记录管理',
    eyebrow: '心率数据',
    description: '维护手动录入和摄像头测量保存的心率记录。',
    allowCreate: false,
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('phone', '手机号', '140px'),
      col('heart_rate', '心率', '120px'),
      col('measure_type', '测量类型', '130px'),
      col('record_time', '记录时间', '180px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('heart_rate', '心率', 'number'),
      field('measure_type', '测量类型', 'select', {
        options: [
          { label: '手动', value: 'manual' },
          { label: '摄像头', value: 'camera_ppg' }
        ]
      }),
      field('record_time', '记录时间')
    ]
  },
  'user-plans': {
    key: 'user-plans',
    resource: 'user-plans',
    title: '用户计划管理',
    eyebrow: '计划数据',
    description: '维护用户创建后的健康计划和进度。',
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('title', '计划标题', '180px'),
      col('category', '分类', '100px'),
      col('target_days', '目标天数', '100px'),
      col('progress', '进度', '90px'),
      col('status', '状态', '110px'),
      col('start_date', '开始日期', '120px'),
      col('end_date', '结束日期', '120px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('title', '计划标题'),
      field('category', '分类'),
      field('target_days', '目标天数', 'number'),
      field('progress', '进度', 'number'),
      field('status', '状态', 'select', {
        options: [
          { label: '进行中', value: 'active' },
          { label: '已完成', value: 'finished' },
          { label: '已删除', value: 'deleted' }
        ]
      }),
      field('start_date', '开始日期'),
      field('end_date', '结束日期'),
      field('goal_text', '目标说明', 'textarea', { full: true }),
      field('theme', '主题')
    ]
  },
  'plan-checkins': {
    key: 'plan-checkins',
    resource: 'plan-checkins',
    title: '打卡记录管理',
    eyebrow: '打卡数据',
    description: '维护用户计划每日打卡记录。',
    columns: [
      col('id', '编号', '90px'),
      col('plan_id', '计划编号', '100px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('checkin_date', '打卡日期', '130px'),
      col('remark', '打卡内容', '260px', true),
      col('created_at', '创建时间', '180px')
    ],
    fields: [
      field('plan_id', '计划编号', 'number'),
      field('user_id', '用户编号', 'number'),
      field('checkin_date', '打卡日期'),
      field('remark', '打卡内容', 'textarea', { full: true })
    ]
  },
  'article-favorites': {
    key: 'article-favorites',
    resource: 'article-favorites',
    title: '收藏管理',
    eyebrow: '收藏数据',
    description: '维护用户收藏的文章记录。',
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('article_title', '文章标题', '220px', true),
      col('article_url', '链接', '260px', true),
      col('source', '来源', '120px'),
      col('favorite_time', '收藏时间', '180px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('article_title', '文章标题'),
      field('article_url', '文章链接'),
      field('source', '来源'),
      field('favorite_time', '收藏时间'),
      field('summary', '摘要', 'textarea', { full: true })
    ]
  },
  'user-settings': {
    key: 'user-settings',
    resource: 'user-settings',
    title: '用户设置管理',
    eyebrow: '设置数据',
    description: '维护用户目标、通知与隐私设置。',
    allowCreate: false,
    columns: [
      col('id', '编号', '90px'),
      col('user_id', '用户编号', '100px'),
      col('username', '用户名', '120px'),
      col('phone', '手机号', '140px'),
      col('nickname', '昵称', '130px'),
      col('notify_enabled', '通知', '90px'),
      col('step_goal', '步数目标', '110px'),
      col('privacy_level', '隐私级别', '120px'),
      col('updated_at', '更新时间', '180px')
    ],
    fields: [
      field('user_id', '用户编号', 'number'),
      field('notify_enabled', '通知', 'select', { options: yesNoOptions }),
      field('step_goal', '步数目标', 'number'),
      field('privacy_level', '隐私级别', 'select', { options: privacyOptions })
    ]
  }
}
