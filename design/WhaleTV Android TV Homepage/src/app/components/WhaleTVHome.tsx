import { Search, Heart, Clock, Settings, Lock, Edit, RefreshCw, Tv, Radio, Newspaper, Trophy, Film, Music, Baby, FileVideo, Smile, FolderOpen, Calendar } from 'lucide-react';
import { useState } from 'react';

interface Channel {
  id: string;
  name: string;
  logo: string;
  logoAbbr: string;
  category: string;
  sources: number;
  hasEpg: boolean;
  quality: '4K' | 'HD' | 'SD';
  available: boolean;
}

const categories = [
  { id: 'all', name: '全部', icon: FolderOpen, count: 428 },
  { id: 'general', name: '综合', icon: Tv, count: 86 },
  { id: 'news', name: '新闻', icon: Newspaper, count: 128 },
  { id: 'sports', name: '体育', icon: Trophy, count: 42 },
  { id: 'movies', name: '电影', icon: Film, count: 31 },
  { id: 'music', name: '音乐', icon: Music, count: 24 },
  { id: 'kids', name: '少儿', icon: Baby, count: 18 },
  { id: 'documentary', name: '纪录片', icon: FileVideo, count: 16 },
  { id: 'entertainment', name: '娱乐', icon: Smile, count: 35 },
  { id: 'uncategorized', name: '未分类', icon: Radio, count: 12 },
];

const countries = [
  { id: 'cn', name: '中国', locked: true },
  { id: 'us', name: '美国', locked: false },
  { id: 'jp', name: '日本', locked: false },
  { id: 'uk', name: '英国', locked: false },
  { id: 'kr', name: '韩国', locked: false },
];

const channels: Channel[] = [
  { id: '1', name: 'CCTV-13 新闻', logo: 'CCTV-13', logoAbbr: '13', category: '新闻', sources: 3, hasEpg: true, quality: '4K', available: true },
  { id: '2', name: 'CGTN', logo: 'CGTN', logoAbbr: 'CGTN', category: '新闻', sources: 2, hasEpg: true, quality: 'HD', available: true },
  { id: '3', name: '新华社电视', logo: '新华社', logoAbbr: '新华', category: '新闻', sources: 1, hasEpg: true, quality: 'HD', available: true },
  { id: '4', name: '凤凰资讯', logo: '凤凰资讯', logoAbbr: '凤凰', category: '新闻', sources: 4, hasEpg: false, quality: 'HD', available: true },
  { id: '5', name: 'CCTV-4 中文国际', logo: 'CCTV-4', logoAbbr: '4', category: '综合', sources: 5, hasEpg: true, quality: 'HD', available: true },
  { id: '6', name: 'CCTV-1 综合', logo: 'CCTV-1', logoAbbr: '1', category: '综合', sources: 2, hasEpg: true, quality: 'HD', available: true },
  { id: '7', name: 'CCTV-新闻', logo: 'CCTV新闻', logoAbbr: '新闻', category: '新闻', sources: 3, hasEpg: false, quality: 'HD', available: true },
  { id: '8', name: 'CCTV-英语', logo: 'CCTV-E', logoAbbr: 'E', category: '新闻', sources: 2, hasEpg: true, quality: 'HD', available: true },
];

function ChannelLogoBlock({ channel, isFocused }: { channel: Channel; isFocused: boolean }) {
  const isCCTV = channel.logo.startsWith('CCTV') || channel.logo === 'CCTV新闻';
  const isCGTN = channel.logo === 'CGTN';

  return (
    <div
      className="flex items-center justify-center relative"
      style={{
        height: '130px',
        background: 'linear-gradient(160deg, #111620 0%, #0d1119 100%)',
      }}
    >
      {/* subtle grid texture */}
      <div
        className="absolute inset-0 opacity-[0.03]"
        style={{
          backgroundImage: 'repeating-linear-gradient(0deg, #fff 0px, transparent 1px, transparent 20px), repeating-linear-gradient(90deg, #fff 0px, transparent 1px, transparent 20px)',
        }}
      />

      {/* Logo badge container */}
      <div
        className="relative z-10 flex flex-col items-center justify-center rounded-xl"
        style={{
          width: '108px',
          height: '78px',
          background: 'linear-gradient(135deg, #1a2540 0%, #0f1a30 100%)',
          border: isFocused
            ? '1px solid rgba(0,200,212,0.35)'
            : '1px solid rgba(255,255,255,0.06)',
          boxShadow: isFocused
            ? '0 0 16px rgba(0,200,212,0.15), inset 0 1px 0 rgba(255,255,255,0.05)'
            : 'inset 0 1px 0 rgba(255,255,255,0.04)',
        }}
      >
        {isCCTV ? (
          <div className="flex flex-col items-center gap-0.5">
            <span style={{ color: '#c8d4e8', fontSize: '11px', letterSpacing: '0.15em', opacity: 0.7 }}>CCTV</span>
            <span style={{ color: '#e8f0ff', fontSize: '22px', lineHeight: 1, letterSpacing: '0.05em', fontWeight: 700 }}>
              {channel.logoAbbr}
            </span>
          </div>
        ) : isCGTN ? (
          <span style={{ color: '#c8e0ff', fontSize: '20px', fontWeight: 800, letterSpacing: '0.08em' }}>CGTN</span>
        ) : (
          <span style={{ color: '#e8f0ff', fontSize: '18px', fontWeight: 700 }}>{channel.logo}</span>
        )}
      </div>

      {/* quality badge top-right */}
      <div
        className="absolute top-3 right-3 rounded px-2 py-1 font-bold"
        style={{
          backgroundColor: channel.quality === '4K' ? 'rgba(0,200,212,0.18)' : 'rgba(255,255,255,0.07)',
          color: channel.quality === '4K' ? '#00c8d4' : '#99aec8',
          border: channel.quality === '4K' ? '1px solid rgba(0,200,212,0.35)' : '1px solid rgba(255,255,255,0.09)',
          fontSize: '12px',
          letterSpacing: '0.06em',
        }}
      >
        {channel.quality === 'HD' ? '高清' : channel.quality}
      </div>
    </div>
  );
}

export function WhaleTVHome() {
  const [selectedCountry, setSelectedCountry] = useState('cn');
  const [selectedCategory, setSelectedCategory] = useState('news');
  const [focusedChannel, setFocusedChannel] = useState('1'); // CCTV-13 新闻

  const currentTime = '20:30';
  const lastSyncTime = '20:25';
  const channelCount = 128;

  return (
    <div className="w-full h-full flex flex-col" style={{ backgroundColor: '#0d0f12' }}>
      {/* 第一层：顶部全局工具栏 */}
      <header className="flex items-center justify-between px-12 h-[52px] shrink-0" style={{ backgroundColor: '#151921' }}>
        <div className="flex items-center gap-3">
          <Tv className="w-7 h-7" style={{ color: '#00c8d4' }} />
          <span className="text-xl font-semibold" style={{ color: '#dde4f0' }}>WhaleTV</span>
        </div>

        <div className="flex items-center gap-8">
          {[
            { icon: Search, label: '搜索' },
            { icon: Heart, label: '收藏' },
            { icon: Clock, label: '历史' },
            { icon: Settings, label: '设置' },
          ].map(({ icon: Icon, label }) => (
            <button key={label} className="flex items-center gap-2 px-3 py-1.5 rounded hover:bg-white/5 transition-colors">
              <Icon className="w-5 h-5" style={{ color: '#6b7fa3' }} />
              <span className="text-sm" style={{ color: '#dde4f0' }}>{label}</span>
            </button>
          ))}

          <div className="flex items-center gap-4 ml-4 pl-4 border-l" style={{ borderColor: '#1c2233' }}>
            <div className="flex items-center gap-1.5">
              <div className="w-2 h-2 rounded-full" style={{ backgroundColor: '#22c55e' }}></div>
              <span className="text-sm" style={{ color: '#8899bb' }}>已同步</span>
            </div>
            <span className="text-sm font-medium" style={{ color: '#dde4f0' }}>{currentTime}</span>
          </div>
        </div>
      </header>

      {/* 第二层：国家 Tab 层 */}
      <div className="flex items-center gap-3 px-12 h-[56px] shrink-0" style={{ backgroundColor: '#151921', borderTop: '1px solid #1c2233' }}>
        {countries.map((country) => {
          const isSelected = selectedCountry === country.id;
          return (
            <button
              key={country.id}
              onClick={() => setSelectedCountry(country.id)}
              className="flex items-center gap-2 px-5 py-2 rounded-full transition-all"
              style={{
                backgroundColor: isSelected ? 'rgba(0,200,212,0.10)' : 'transparent',
                color: isSelected ? '#00c8d4' : '#7a8eaa',
                border: isSelected ? '1.5px solid rgba(0,200,212,0.55)' : '1.5px solid transparent',
                boxShadow: isSelected ? '0 0 10px rgba(0,200,212,0.12)' : 'none',
              }}
            >
              {country.locked && <Lock className="w-3.5 h-3.5" />}
              <span className="text-base font-medium">{country.name}</span>
            </button>
          );
        })}

        <button className="flex items-center gap-2 px-5 py-2 rounded-md transition-all hover:bg-white/5" style={{ color: '#6b7fa3' }}>
          <Edit className="w-4 h-4" />
          <span className="text-base">编辑</span>
        </button>
      </div>

      {/* 第三层：主内容区 */}
      <div className="flex flex-1 overflow-hidden">
        {/* 左侧分类栏 */}
        <aside className="w-[220px] shrink-0 py-5 px-3" style={{ backgroundColor: '#151921' }}>
          <div className="space-y-0.5">
            {categories.map((category) => {
              const Icon = category.icon;
              const isSelected = selectedCategory === category.id;

              return (
                <button
                  key={category.id}
                  onClick={() => setSelectedCategory(category.id)}
                  className="w-full flex items-center py-2.5 rounded-lg transition-all relative"
                  style={{
                    backgroundColor: isSelected ? '#1c2233' : 'transparent',
                    paddingLeft: '28px',
                    paddingRight: '18px',
                  }}
                >
                  {isSelected && (
                    <div
                      className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-7 rounded-r"
                      style={{ backgroundColor: '#00c8d4' }}
                    />
                  )}
                  {/* 左侧组：图标 + 文字紧挨 */}
                  <Icon
                    style={{ width: '20px', height: '20px', flexShrink: 0, color: isSelected ? '#00c8d4' : '#7a8eaa' }}
                  />
                  <span
                    className="text-base font-medium"
                    style={{ color: isSelected ? '#00c8d4' : '#c8d4e4', marginLeft: '12px' }}
                  >
                    {category.name}
                  </span>
                  {/* 右侧：数量推到最右 */}
                  <span
                    className="text-sm tabular-nums"
                    style={{ marginLeft: 'auto', color: isSelected ? 'rgba(0,200,212,0.75)' : '#5e7090' }}
                  >
                    {category.count}
                  </span>
                </button>
              );
            })}
          </div>
        </aside>

        {/* 右侧频道内容区 */}
        <main className="flex-1 overflow-hidden flex flex-col py-5 px-7">
          {/* 内容区标题 */}
          <div className="flex items-center justify-between mb-5 shrink-0">
            <div className="flex items-center gap-4">
              <h1 className="text-2xl font-semibold" style={{ color: '#dde4f0' }}>
                中国 · 新闻
              </h1>
              <span className="text-base" style={{ color: '#6b7fa3' }}>
                {channelCount} 个频道
              </span>
            </div>

            <div className="flex items-center gap-3">
              <span className="text-sm" style={{ color: '#6b7fa3' }}>
                最近同步 {lastSyncTime}
              </span>
              <button className="p-2 rounded-md hover:bg-white/5 transition-colors">
                <RefreshCw className="w-4 h-4" style={{ color: '#6b7fa3' }} />
              </button>
            </div>
          </div>

          {/* 频道网格 — 填满剩余高度 */}
          <div className="grid grid-cols-4 gap-5 flex-1" style={{ gridTemplateRows: '1fr 1fr' }}>
            {channels.map((channel) => {
              const isFocused = focusedChannel === channel.id;

              return (
                <div
                  key={channel.id}
                  onClick={() => setFocusedChannel(channel.id)}
                  className="rounded-xl overflow-hidden cursor-pointer flex flex-col"
                  style={{
                    backgroundColor: '#1c2233',
                    border: isFocused ? '2px solid #00c8d4' : '2px solid rgba(255,255,255,0.04)',
                    transform: isFocused ? 'scale(1.03)' : 'scale(1)',
                    boxShadow: isFocused
                      ? '0 6px 28px rgba(0, 200, 212, 0.22), 0 0 0 1px rgba(0,200,212,0.1)'
                      : '0 2px 8px rgba(0,0,0,0.3)',
                    transition: 'transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease',
                  }}
                >
                  {/* Logo 区域 */}
                  <ChannelLogoBlock channel={channel} isFocused={isFocused} />

                  {/* 信息区域 */}
                  <div
                    className="px-4 py-3 flex flex-col gap-1.5 flex-1"
                    style={{ backgroundColor: '#1c2233' }}
                  >
                    {/* 频道名 */}
                    <div
                      className="text-base font-semibold truncate"
                      style={{ color: isFocused ? '#e8f4f5' : '#d0dce8' }}
                    >
                      {channel.name}
                    </div>

                    {/* 分类 */}
                    <div className="text-sm" style={{ color: '#7a8eaa' }}>
                      {channel.category}
                    </div>

                    {/* 状态信息行 — 单行，不换行 */}
                    <div
                      className="flex items-center gap-2 whitespace-nowrap overflow-hidden"
                      style={{ color: '#8899bb', fontSize: '12px' }}
                    >
                      {channel.available && (
                        <div className="flex items-center gap-1 shrink-0">
                          <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: '#22c55e' }}></div>
                          <span style={{ color: '#7acea0' }}>可用</span>
                        </div>
                      )}

                      <span className="shrink-0" style={{ color: '#8899bb' }}>{channel.sources} 个源</span>

                      {channel.hasEpg && (
                        <div className="flex items-center gap-1 shrink-0">
                          <Calendar className="w-3 h-3" style={{ color: '#8899bb' }} />
                          <span>EPG</span>
                        </div>
                      )}
                    </div>

                    {/* 当前节目 — 仅焦点卡片显示 */}
                    {isFocused && (
                      <div
                        className="flex items-center gap-1.5 truncate mt-0.5"
                        style={{ fontSize: '12px', color: '#5fc8b8' }}
                      >
                        <div className="w-1 h-1 rounded-full shrink-0" style={{ backgroundColor: '#5fc8b8' }} />
                        <span className="truncate">正在播出：新闻直播间</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </main>
      </div>
    </div>
  );
}
