import { useState } from 'react';
import { ArrowLeft, Search, X, Clock, TrendingUp, Star } from 'lucide-react';
import { CHANNELS, Channel } from './tvData';
import { ChannelLogo, LiveBadge, QualityBadge, CategoryChip } from './HomeScreen';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';

interface SearchScreenProps {
  navigate: (screen: Screen, channel?: Channel) => void;
}

const RECENT: string[] = ['CCTV-5', '湖南卫视', '新闻', '体育', '纪录片'];
const CHINESE_QUICK = ['央视', '卫视', '体育', '新闻', '纪录', '少儿', '地方', '综合'];
const PINYIN: string[][] = [
  ['A','B','C','D','E','F','G'],
  ['H','I','J','K','L','M','N'],
  ['O','P','Q','R','S','T','U'],
  ['V','W','X','Y','Z','⌫','清空'],
];

function SearchCard({ ch, focused, onClick }: { ch: Channel; focused: boolean; onClick: () => void }) {
  return (
    <div
      tabIndex={0}
      onFocus={() => {}}
      onClick={onClick}
      style={{
        width: 196,
        background: focused ? '#1c2233' : '#13171f',
        border: `1px solid ${focused ? '#00c8d4' : 'rgba(255,255,255,0.06)'}`,
        borderRadius: 8, padding: '14px',
        cursor: 'pointer', outline: 'none',
        transform: focused ? 'scale(1.04)' : 'scale(1)',
        boxShadow: focused
          ? '0 0 0 1px #00c8d4, 0 8px 24px rgba(0,200,212,0.18)'
          : '0 2px 8px rgba(0,0,0,0.3)',
        transition: 'all 0.12s',
        display: 'flex', flexDirection: 'column', gap: 8,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <ChannelLogo ch={ch} size={40} />
        <div style={{ minWidth: 0 }}>
          <div style={{ color: '#dde4f0', fontSize: 12, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {ch.name}
          </div>
          <div style={{ color: '#6b7fa3', fontSize: 10, marginTop: 1 }}>{ch.categoryLabel}</div>
        </div>
        {ch.isFavorite && <Star size={11} style={{ color: '#d4a017', fill: '#d4a017', marginLeft: 'auto', flexShrink: 0 }} />}
      </div>
      <div style={{ display: 'flex', gap: 5, alignItems: 'center' }}>
        <LiveBadge />
        <QualityBadge q={ch.quality} />
        <span style={{ color: '#6b7fa3', fontSize: 10, marginLeft: 'auto' }}>{ch.sourceCount} 源</span>
      </div>
    </div>
  );
}

export function SearchScreen({ navigate }: SearchScreenProps) {
  const [query, setQuery] = useState('');
  const [focusedKey, setFocusedKey] = useState<string | null>(null);
  const [focusedResult, setFocusedResult] = useState<string | null>(null);
  const [backFocused, setBackFocused] = useState(false);

  const results = query.length > 0
    ? CHANNELS.filter(c =>
        c.name.toLowerCase().includes(query.toLowerCase()) ||
        c.shortName.toLowerCase().includes(query.toLowerCase()) ||
        c.categoryLabel.includes(query) ||
        c.categoryId.includes(query)
      )
    : [];

  const handleKey = (k: string) => {
    if (k === '⌫') setQuery(q => q.slice(0, -1));
    else if (k === '清空') setQuery('');
    else setQuery(q => q + k);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Top bar */}
      <div style={{
        height: 52, display: 'flex', alignItems: 'center', padding: '0 26px', gap: 12,
        borderBottom: '1px solid rgba(255,255,255,0.05)', flexShrink: 0, background: '#0f1218',
      }}>
        <button
          tabIndex={0}
          onFocus={() => setBackFocused(true)}
          onBlur={() => setBackFocused(false)}
          onClick={() => navigate('home')}
          style={{
            background: backFocused ? 'rgba(0,200,212,0.1)' : 'transparent',
            border: `1px solid ${backFocused ? '#00c8d4' : 'rgba(255,255,255,0.1)'}`,
            borderRadius: 6, padding: '6px 12px', cursor: 'pointer', outline: 'none',
            color: backFocused ? '#00c8d4' : '#8899b4',
            display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, transition: 'all 0.1s',
          }}
        >
          <ArrowLeft size={14} /> 返回
        </button>
        <span style={{ color: '#dde4f0', fontSize: 14, fontWeight: 600 }}>搜索频道</span>
      </div>

      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Left: input + keyboard */}
        <div style={{
          width: 400, flexShrink: 0,
          borderRight: '1px solid rgba(255,255,255,0.05)',
          padding: '22px 22px', display: 'flex', flexDirection: 'column', gap: 14,
        }}>
          {/* Search input display */}
          <div style={{
            background: '#13171f', border: '1px solid rgba(0,200,212,0.35)',
            borderRadius: 8, padding: '13px 16px',
            display: 'flex', alignItems: 'center', gap: 10,
            boxShadow: '0 0 0 1px rgba(0,200,212,0.12)',
          }}>
            <Search size={17} style={{ color: '#00c8d4', flexShrink: 0 }} />
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', minHeight: 22 }}>
              {query
                ? <span style={{ color: '#dde4f0', fontSize: 17, fontWeight: 500 }}>{query}</span>
                : <span style={{ color: '#2a3a50', fontSize: 15 }}>输入频道名称或拼音…</span>
              }
              <div style={{ width: 2, height: 18, background: '#00c8d4', marginLeft: 2, animation: 'blink 1s ease infinite' }} />
            </div>
            {query && (
              <button onClick={() => setQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6b7fa3', padding: 2, display: 'flex' }}>
                <X size={15} />
              </button>
            )}
          </div>

          {/* Chinese quick chips */}
          <div>
            <div style={{ color: '#6b7fa3', fontSize: 9, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 7 }}>快速选择</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 5 }}>
              {CHINESE_QUICK.map(s => {
                const fk = `quick-${s}`;
                return (
                  <button
                    key={s}
                    tabIndex={0}
                    onClick={() => setQuery(s)}
                    onFocus={() => setFocusedKey(fk)}
                    onBlur={() => setFocusedKey(null)}
                    style={{
                      background: focusedKey === fk ? 'rgba(0,200,212,0.1)' : '#13171f',
                      border: `1px solid ${focusedKey === fk ? '#00c8d4' : 'rgba(255,255,255,0.08)'}`,
                      borderRadius: 5, padding: '7px 13px', cursor: 'pointer', outline: 'none',
                      color: focusedKey === fk ? '#00c8d4' : '#8899b4', fontSize: 12, fontWeight: 500,
                      boxShadow: focusedKey === fk ? '0 0 0 1px #00c8d4' : 'none',
                      transition: 'all 0.09s',
                      transform: focusedKey === fk ? 'scale(1.06)' : 'scale(1)',
                      fontFamily: 'Noto Sans SC, sans-serif',
                    }}
                  >
                    {s}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Pinyin keyboard */}
          <div>
            <div style={{ color: '#6b7fa3', fontSize: 9, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 7 }}>拼音输入</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {PINYIN.map((row, ri) => (
                <div key={ri} style={{ display: 'flex', gap: 4 }}>
                  {row.map(k => {
                    const fk = `key-${k}`;
                    const isSpecial = k === '⌫' || k === '清空';
                    return (
                      <button
                        key={k}
                        tabIndex={0}
                        onFocus={() => setFocusedKey(fk)}
                        onBlur={() => setFocusedKey(null)}
                        onClick={() => handleKey(k)}
                        style={{
                          flex: isSpecial ? 1.6 : 1, height: 38,
                          background: focusedKey === fk ? '#1c2233' : '#13171f',
                          border: `1px solid ${focusedKey === fk ? '#00c8d4' : 'rgba(255,255,255,0.07)'}`,
                          borderRadius: 5, cursor: 'pointer', outline: 'none',
                          color: isSpecial
                            ? (focusedKey === fk ? '#e53535' : '#6b7fa3')
                            : (focusedKey === fk ? '#00c8d4' : '#b8c4d8'),
                          fontSize: k === '清空' ? 10 : 12, fontWeight: 500,
                          boxShadow: focusedKey === fk ? '0 0 0 1px #00c8d4' : 'none',
                          transition: 'all 0.09s',
                          transform: focusedKey === fk ? 'scale(1.07)' : 'scale(1)',
                        }}
                      >
                        {k}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right: results or empty state */}
        <div style={{ flex: 1, padding: '22px 26px', overflowY: 'auto' }}>
          {results.length > 0 ? (
            <div>
              <div style={{ color: '#6b7fa3', fontSize: 12, marginBottom: 14 }}>
                找到 <span style={{ color: '#00c8d4', fontWeight: 600 }}>{results.length}</span> 个频道
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                {results.map(ch => (
                  <div
                    key={ch.id}
                    onFocus={() => setFocusedResult(ch.id)}
                    onBlur={() => setFocusedResult(null)}
                  >
                    <SearchCard
                      ch={ch}
                      focused={focusedResult === ch.id}
                      onClick={() => navigate('player', ch)}
                    />
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div>
              {/* Recent searches */}
              <div style={{ marginBottom: 26 }}>
                <div style={{ display: 'flex', gap: 7, alignItems: 'center', marginBottom: 10 }}>
                  <Clock size={13} style={{ color: '#6b7fa3' }} />
                  <span style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 1, textTransform: 'uppercase' }}>最近搜索</span>
                </div>
                <div style={{ display: 'flex', gap: 7, flexWrap: 'wrap' }}>
                  {RECENT.map(s => {
                    const fk = `recent-${s}`;
                    return (
                      <button
                        key={s}
                        tabIndex={0}
                        onClick={() => setQuery(s)}
                        onFocus={() => setFocusedKey(fk)}
                        onBlur={() => setFocusedKey(null)}
                        style={{
                          background: focusedKey === fk ? '#1c2233' : '#13171f',
                          border: `1px solid ${focusedKey === fk ? '#00c8d4' : 'rgba(255,255,255,0.07)'}`,
                          borderRadius: 5, padding: '7px 15px', cursor: 'pointer', outline: 'none',
                          color: focusedKey === fk ? '#00c8d4' : '#8899b4', fontSize: 12,
                          display: 'flex', alignItems: 'center', gap: 6,
                          boxShadow: focusedKey === fk ? '0 0 0 1px #00c8d4' : 'none',
                          transition: 'all 0.09s',
                          transform: focusedKey === fk ? 'scale(1.04)' : 'scale(1)',
                        }}
                      >
                        <Clock size={10} style={{ opacity: 0.5 }} /> {s}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Hot channels */}
              <div>
                <div style={{ display: 'flex', gap: 7, alignItems: 'center', marginBottom: 10 }}>
                  <TrendingUp size={13} style={{ color: '#6b7fa3' }} />
                  <span style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 1, textTransform: 'uppercase' }}>热门频道</span>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                  {CHANNELS.filter(c => c.isFavorite || c.categoryId === 'cctv').slice(0, 6).map(ch => (
                    <div
                      key={ch.id}
                      tabIndex={0}
                      onFocus={() => setFocusedResult(`hot-${ch.id}`)}
                      onBlur={() => setFocusedResult(null)}
                      onClick={() => navigate('player', ch)}
                      style={{
                        display: 'flex', alignItems: 'center', gap: 10, minWidth: 190,
                        background: focusedResult === `hot-${ch.id}` ? '#1c2233' : '#13171f',
                        border: `1px solid ${focusedResult === `hot-${ch.id}` ? '#00c8d4' : 'rgba(255,255,255,0.06)'}`,
                        borderRadius: 7, padding: '10px 13px', cursor: 'pointer', outline: 'none',
                        boxShadow: focusedResult === `hot-${ch.id}` ? '0 0 0 1px #00c8d4, 0 4px 16px rgba(0,200,212,0.15)' : 'none',
                        transition: 'all 0.1s',
                        transform: focusedResult === `hot-${ch.id}` ? 'scale(1.03)' : 'scale(1)',
                      }}
                    >
                      <ChannelLogo ch={ch} size={34} />
                      <div style={{ minWidth: 0 }}>
                        <div style={{ color: '#dde4f0', fontSize: 12, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {ch.name}
                        </div>
                        <div style={{ display: 'flex', gap: 5, marginTop: 3 }}>
                          <CategoryChip label={ch.categoryLabel} />
                          <span style={{ color: '#6b7fa3', fontSize: 9 }}>{ch.sourceCount} 源</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
      `}</style>
    </div>
  );
}
