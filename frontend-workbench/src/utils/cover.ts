/**
 * 无封面作品的占位配色。
 *
 * 色系参考 https://www.jianshu.com/p/48be69b211a3 里提到的莫兰迪 / 马卡龙 / 多巴胺三档，
 * 按「越靠前越沉稳」排列：企业内网以莫兰迪为主，多巴胺只留少量点缀，避免整屏高饱和刺眼。
 *
 * 首字统一用深色 rgba(31,41,55,.8)：这 20 个色都是中高明度，实测深色字对比度 3.42~5.72:1，
 * 全部达到 WCAG 大字 3:1 门槛；同样条件下白字只有 2.57~2.93:1，反而不达标。
 * 往调色板里加深色时需要同步复核这一点。
 */
const PALETTE: readonly string[] = [
  // 莫兰迪（低饱和灰调）
  '#8D9CB5', '#A7B6A2', '#C0AFA0', '#BFA3A3', '#9C96B5',
  '#8FA9A0', '#B7A98C', '#A29BB0', '#7E93A8', '#B99C8B',
  // 马卡龙（柔粉彩）
  '#F2A0B5', '#8FC7E8', '#A9D6A0', '#F0C879', '#BDA7E0', '#8FD9C8',
  // 多巴胺（高饱和点缀）
  '#F2784B', '#4B9FE1', '#E85D9B', '#3FBFA0'
]

const FOREGROUND = 'rgba(31, 41, 55, 0.8)'

/**
 * FNV-1a 加一道 xorshift 终混。
 *
 * 终混不是可选的：FNV-1a 低位熵很差，直接 `% 20` 实测只命中偶数下标，
 * 20 色里有 10 色永远取不到（卡方 31915）；混过之后 20 色全覆盖、卡方 17.8、近似均匀。
 */
function hash(seed: string): number {
  let h = 0x811c9dc5
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  h ^= h >>> 16
  h = Math.imul(h, 0x21f0aaad)
  h ^= h >>> 15
  h = Math.imul(h, 0x735a2d97)
  h ^= h >>> 15
  return h >>> 0
}

export interface CoverPlaceholderStyle {
  backgroundColor: string
  color: string
}

/**
 * 取某个作品的占位底色。
 *
 * 种子用 id + 标题而非 Math.random()：随机数会让同一作品在列表页与详情页取到不同颜色，
 * 且每次重渲染都跳变，看起来像在闪。哈希保证「每个作品颜色各异，但永远不变」；
 * 标题相同的两个作品（现网就有 3 条都叫「智能待办工作台」）也因为带了 id 而能取到不同色。
 */
export function coverPlaceholderStyle(
  id: string | number,
  title: string
): CoverPlaceholderStyle {
  return {
    backgroundColor: PALETTE[hash(`${id}:${title}`) % PALETTE.length],
    color: FOREGROUND
  }
}
