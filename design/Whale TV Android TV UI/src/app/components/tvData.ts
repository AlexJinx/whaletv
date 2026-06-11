export interface StreamSource {
  label: string;
  quality?: 'HD' | '4K' | 'SD';
  status: 'unchecked' | 'ok' | 'fair' | 'unavailable';
}

export interface EpgProgram {
  time: string;
  end: string;
  name: string;
  desc?: string;
  status: 'past' | 'current' | 'next' | 'future';
  progress?: number; // 0–100, only on current
}

export interface Channel {
  id: string;
  name: string;
  shortName: string;
  categoryId: string;
  categoryLabel: string;
  quality?: 'HD' | '4K' | 'SD';
  isFavorite: boolean;
  sourceCount: number;
  sources: StreamSource[];
  logoColor: string;
  logoText: string;
  // EPG — present only when XMLTV configured and matched
  epg?: EpgProgram[];
}

export const CHANNELS: Channel[] = [
  {
    id: 'cctv1',
    name: 'CCTV-1 综合',
    shortName: 'CCTV-1',
    categoryId: 'cctv',
    categoryLabel: '央视',
    quality: 'HD',
    isFavorite: true,
    sourceCount: 3,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'fair' },
      { label: '源 3', quality: 'SD', status: 'unavailable' },
    ],
    logoColor: '#c41e3a',
    logoText: '综合',
  },
  {
    id: 'cctv4',
    name: 'CCTV-4 中文国际',
    shortName: 'CCTV-4',
    categoryId: 'cctv',
    categoryLabel: '央视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', status: 'unchecked' },
    ],
    logoColor: '#c41e3a',
    logoText: '国际',
  },
  {
    id: 'cctv5',
    name: 'CCTV-5 体育',
    shortName: 'CCTV-5',
    categoryId: 'sports',
    categoryLabel: '体育',
    quality: '4K',
    isFavorite: true,
    sourceCount: 3,
    sources: [
      { label: '源 1', quality: '4K', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'ok' },
      { label: '源 3', quality: 'HD', status: 'unchecked' },
    ],
    logoColor: '#0057a8',
    logoText: '体育',
  },
  {
    id: 'cctv6',
    name: 'CCTV-6 电影',
    shortName: 'CCTV-6',
    categoryId: 'cctv',
    categoryLabel: '央视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'SD', status: 'fair' },
    ],
    logoColor: '#c41e3a',
    logoText: '电影',
  },
  {
    id: 'cctv9',
    name: 'CCTV-9 纪录',
    shortName: 'CCTV-9',
    categoryId: 'documentary',
    categoryLabel: '纪录片',
    quality: '4K',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: '4K', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'unavailable' },
    ],
    logoColor: '#2e7d32',
    logoText: '纪录',
  },
  {
    id: 'cctv13',
    name: 'CCTV-13 新闻',
    shortName: 'CCTV-13',
    categoryId: 'news',
    categoryLabel: '新闻',
    quality: 'HD',
    isFavorite: true,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'ok' },
    ],
    logoColor: '#c41e3a',
    logoText: '新闻',
  },
  {
    id: 'cctv14',
    name: 'CCTV-14 少儿',
    shortName: 'CCTV-14',
    categoryId: 'kids',
    categoryLabel: '少儿',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 1,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
    ],
    logoColor: '#f9a825',
    logoText: '少儿',
  },
  {
    id: 'hunan',
    name: '湖南卫视',
    shortName: '湖南卫视',
    categoryId: 'satellite',
    categoryLabel: '卫视',
    quality: 'HD',
    isFavorite: true,
    sourceCount: 3,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'ok' },
      { label: '源 3', quality: 'SD', status: 'fair' },
    ],
    logoColor: '#e65100',
    logoText: '湖南',
  },
  {
    id: 'dragon',
    name: '东方卫视',
    shortName: '东方卫视',
    categoryId: 'satellite',
    categoryLabel: '卫视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'SD', status: 'unchecked' },
    ],
    logoColor: '#880e4f',
    logoText: '东方',
  },
  {
    id: 'jiangsu',
    name: '江苏卫视',
    shortName: '江苏卫视',
    categoryId: 'satellite',
    categoryLabel: '卫视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'SD', status: 'unavailable' },
    ],
    logoColor: '#1565c0',
    logoText: '江苏',
  },
  {
    id: 'zhejiang',
    name: '浙江卫视',
    shortName: '浙江卫视',
    categoryId: 'satellite',
    categoryLabel: '卫视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 1,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
    ],
    logoColor: '#00695c',
    logoText: '浙江',
  },
  {
    id: 'guangdong',
    name: '广东卫视',
    shortName: '广东卫视',
    categoryId: 'satellite',
    categoryLabel: '卫视',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'SD', status: 'fair' },
    ],
    logoColor: '#4a148c',
    logoText: '广东',
  },
  {
    id: 'beijing',
    name: '北京卫视',
    shortName: '北京卫视',
    categoryId: 'local',
    categoryLabel: '地方',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'ok' },
      { label: '源 2', quality: 'HD', status: 'ok' },
    ],
    logoColor: '#b71c1c',
    logoText: '北京',
  },
  {
    id: 'shenzhen',
    name: '深圳卫视',
    shortName: '深圳卫视',
    categoryId: 'local',
    categoryLabel: '地方',
    quality: 'HD',
    isFavorite: false,
    sourceCount: 2,
    sources: [
      { label: '源 1', quality: 'HD', status: 'unavailable' },
      { label: '源 2', quality: 'SD', status: 'fair' },
    ],
    logoColor: '#006064',
    logoText: '深圳',
  },
];

// Demo: simulate EPG available for cctv5 only
export const DEMO_EPG: Record<string, import('./tvData').EpgProgram[]> = {
  cctv5: [
    { time: '18:30', end: '19:30', name: '体育世界', status: 'past' },
    { time: '19:30', end: '21:30', name: '中国足球甲级联赛', status: 'current', progress: 62 },
    { time: '21:30', end: '22:00', name: '体育新闻', status: 'next' },
    { time: '22:00', end: '23:00', name: 'NBA 集锦', status: 'future' },
  ],
};

export type SyncStatus = 'syncing' | 'synced' | 'failed' | 'pending';

export const NAV_SECTIONS = [
  { id: 'recent', label: '继续观看' },
  { id: 'favorite', label: '收藏' },
  { id: 'cctv', label: '央视' },
  { id: 'satellite', label: '卫视' },
  { id: 'local', label: '地方' },
  { id: 'news', label: '新闻' },
  { id: 'sports', label: '体育' },
  { id: 'kids', label: '少儿' },
  { id: 'documentary', label: '纪录片' },
  { id: 'all', label: '全部频道' },
];
