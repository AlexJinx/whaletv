import { useState } from 'react';
import { ArrowLeft, Play, Star, ChevronRight, CalendarOff } from 'lucide-react';
import { Channel, DEMO_EPG } from './tvData';
import { ChannelLogo, LiveBadge, QualityBadge, CategoryChip } from './HomeScreen';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';

interface EPGScreenProps {
  navigate: (screen: Screen, channel?: Channel) => void;
  channel: Channel;
}

function SourceStatusDot({ status }: { status: 'unchecked' | 'ok' | 'fair' | 'unavailable' }) {
  const map = {
    ok:          { color: '#22c55e', label: '可用' },
    fair:        { color: '#d4a017', label: '一般' },
    unavailable: { color: '#e53535', label: '不可用' },
    unchecked:   { color: '#6b7fa3', label: '待检测' },
  };
  const { color, label } = map[status];
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
      <div style={{ width: 6, height: 6, borderRadius: '50%', background: color }} />
      <span style={{ color, fontSize: 11 }}>{label}</span>
    </div>
  );
}

function Tag({ label, color }: { label: string; color: string }) {
  return (
    <span style={{
      color, fontSize: 11, fontWeight: 600,
      border: `1px solid ${color}44`, borderRadius: 4,
      padding: '3px 9px', background: `${color}0f`,
    }}>{label}</span>
  );
}

export function EPGScreen({ navigate, channel }: EPGScreenProps) {
  const [isFavorite, setIsFavorite] = useState(channel.isFavorite);
  const [focusedSrc, setFocusedSrc] = useState<number | null>(null);
  const [backFocused, setBackFocused] = useState(false);
  const [playFocused, setPlayFocused] = useState(false);
  const [favFocused, setFavFocused] = useState(false);

  const epg = DEMO_EPG[channel.id]; // undefined if no EPG configured

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
            display: 'flex', alignItems: 'center', gap: 6, fontSize: 13,
            transition: 'all 0.1s',
          }}
        >
          <ArrowLeft size={14} /> 返回
        </button>
        <div style={{ width: 1, height: 18, background: 'rgba(255,255,255,0.07)' }} />
        <span style={{ color: '#6b7fa3', fontSize: 12 }}>频道详情</span>
        <ChevronRight size={12} style={{ color: '#2a3a50' }} />
        <span style={{ color: '#dde4f0', fontSize: 12, fontWeight: 500 }}>{channel.name}</span>
      </div>

      {/* Body */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Left panel */}
        <div style={{
          width: 348, flexShrink: 0,
          borderRight: '1px solid rgba(255,255,255,0.05)',
          padding: '22px 22px', overflowY: 'auto',
          display: 'flex', flexDirection: 'column', gap: 18,
        }}>
          {/* Channel header */}
          <div style={{
            background: `linear-gradient(135deg, ${channel.logoColor}1e 0%, #13171f 60%)`,
            border: '1px solid rgba(255,255,255,0.07)',
            borderRadius: 8, padding: '18px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 14 }}>
              <div style={{ position: 'relative' }}>
                <ChannelLogo ch={channel} size={60} />
              </div>
              <div>
                <div style={{ color: '#dde4f0', fontSize: 17, fontWeight: 700 }}>{channel.name}</div>
                <div style={{ color: '#6b7fa3', fontSize: 11, marginTop: 2 }}>{channel.categoryLabel}</div>
              </div>
            </div>

            {/* Tags */}
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 14 }}>
              <Tag label="直播" color="#e53535" />
              <Tag label={channel.categoryLabel} color="#00c8d4" />
              {channel.quality && <Tag label={channel.quality} color={channel.quality === '4K' ? '#d4a017' : '#6b7fa3'} />}
              {isFavorite && <Tag label="已收藏" color="#d4a017" />}
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                tabIndex={0}
                onFocus={() => setPlayFocused(true)}
                onBlur={() => setPlayFocused(false)}
                onClick={() => navigate('player', channel)}
                style={{
                  flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
                  background: playFocused ? '#00c8d4' : 'rgba(0,200,212,0.85)',
                  color: '#0d0f12', border: 'none', borderRadius: 6, padding: '9px 0',
                  fontSize: 13, fontWeight: 700, cursor: 'pointer', outline: 'none',
                  boxShadow: playFocused ? '0 0 0 2px #00c8d4, 0 4px 16px rgba(0,200,212,0.28)' : 'none',
                  transition: 'all 0.1s',
                }}
              >
                <Play size={14} style={{ fill: '#0d0f12' }} /> 立即播放
              </button>
              <button
                tabIndex={0}
                onFocus={() => setFavFocused(true)}
                onBlur={() => setFavFocused(false)}
                onClick={() => setIsFavorite(v => !v)}
                style={{
                  background: favFocused ? 'rgba(212,160,23,0.12)' : 'rgba(255,255,255,0.05)',
                  border: `1px solid ${favFocused ? '#d4a017' : isFavorite ? '#d4a01744' : 'rgba(255,255,255,0.1)'}`,
                  borderRadius: 6, padding: '9px 14px', cursor: 'pointer', outline: 'none',
                  display: 'flex', alignItems: 'center', gap: 6, fontSize: 12,
                  color: isFavorite ? '#d4a017' : '#6b7fa3',
                  boxShadow: favFocused ? '0 0 0 1px #d4a017' : 'none',
                  transition: 'all 0.1s',
                }}
              >
                <Star size={14} style={{ fill: isFavorite ? '#d4a017' : 'none' }} />
                {isFavorite ? '已收藏' : '收藏'}
              </button>
            </div>
          </div>

          {/* Stream sources */}
          <div>
            <div style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 8 }}>
              播放源 — {channel.sourceCount} 个
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
              {channel.sources.map((src, i) => (
                <div
                  key={i}
                  tabIndex={0}
                  onFocus={() => setFocusedSrc(i)}
                  onBlur={() => setFocusedSrc(null)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '10px 14px',
                    background: focusedSrc === i ? 'rgba(0,200,212,0.07)' : '#13171f',
                    border: `1px solid ${focusedSrc === i ? '#00c8d4' : 'rgba(255,255,255,0.06)'}`,
                    borderRadius: 6, outline: 'none', cursor: 'default',
                    boxShadow: focusedSrc === i ? '0 0 0 1px #00c8d4' : 'none',
                    transition: 'all 0.1s',
                  }}
                >
                  <span style={{ color: '#dde4f0', fontSize: 13, flex: 1 }}>{src.label}</span>
                  {src.quality && (
                    <span style={{ color: '#6b7fa3', fontSize: 10, border: '1px solid rgba(255,255,255,0.1)', borderRadius: 3, padding: '1px 6px' }}>
                      {src.quality}
                    </span>
                  )}
                  <SourceStatusDot status={src.status} />
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right: EPG or empty state */}
        <div style={{ flex: 1, padding: '22px 26px', overflowY: 'auto' }}>
          <div style={{ color: '#dde4f0', fontSize: 14, fontWeight: 600, marginBottom: 16 }}>节目单</div>

          {epg && epg.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {epg.map((prog, i) => {
                const isCurrent = prog.status === 'current';
                const isPast = prog.status === 'past';
                return (
                  <div
                    key={i}
                    tabIndex={0}
                    style={{
                      display: 'flex', gap: 0,
                      borderBottom: '1px solid rgba(255,255,255,0.04)',
                      opacity: isPast ? 0.4 : 1, outline: 'none',
                    }}
                  >
                    <div style={{ width: 90, flexShrink: 0, padding: '14px 0' }}>
                      <div style={{
                        color: isCurrent ? '#00c8d4' : '#6b7fa3',
                        fontSize: 12, fontFamily: 'JetBrains Mono, monospace', fontWeight: isCurrent ? 600 : 400,
                      }}>
                        {prog.time}
                      </div>
                      <div style={{ color: '#2a3a50', fontSize: 10, fontFamily: 'JetBrains Mono, monospace' }}>
                        {prog.end}
                      </div>
                    </div>
                    <div style={{ width: 22, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '14px 0' }}>
                      <div style={{
                        width: isCurrent ? 9 : 6, height: isCurrent ? 9 : 6, borderRadius: '50%',
                        background: isCurrent ? '#00c8d4' : isPast ? '#1c2a3a' : '#2a3a50',
                        boxShadow: isCurrent ? '0 0 8px rgba(0,200,212,0.5)' : 'none',
                        flexShrink: 0, marginTop: 3,
                      }} />
                      {i < epg.length - 1 && (
                        <div style={{ flex: 1, width: 1, background: isPast ? '#141e2c' : '#1c2a3a', marginTop: 4 }} />
                      )}
                    </div>
                    <div style={{ flex: 1, padding: '14px 0 14px 12px' }}>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 2 }}>
                        <span style={{ color: isCurrent ? '#dde4f0' : '#8899b4', fontSize: 13, fontWeight: isCurrent ? 600 : 400 }}>
                          {prog.name}
                        </span>
                        {isCurrent && (
                          <span style={{
                            background: '#e53535', color: '#fff', fontSize: 9, fontWeight: 700,
                            padding: '2px 6px', borderRadius: 3, letterSpacing: 0.5,
                          }}>
                            直播
                          </span>
                        )}
                        {prog.status === 'next' && (
                          <span style={{
                            color: '#00c8d4', fontSize: 9, border: '1px solid rgba(0,200,212,0.35)',
                            padding: '2px 6px', borderRadius: 3,
                          }}>
                            即将播出
                          </span>
                        )}
                      </div>
                      {prog.desc && <div style={{ color: '#6b7fa3', fontSize: 11 }}>{prog.desc}</div>}
                      {isCurrent && prog.progress != null && (
                        <div style={{ marginTop: 8, height: 2, background: 'rgba(255,255,255,0.08)', borderRadius: 1, maxWidth: 280 }}>
                          <div style={{ width: `${prog.progress}%`, height: '100%', background: '#00c8d4', borderRadius: 1 }} />
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            /* EPG empty state */
            <div style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              gap: 12, padding: '60px 0',
              color: '#6b7fa3',
            }}>
              <CalendarOff size={36} style={{ opacity: 0.4 }} />
              <div style={{ fontSize: 14, fontWeight: 500 }}>暂无节目单数据</div>
              <div style={{ fontSize: 12, color: '#3a4a60', textAlign: 'center', maxWidth: 260, lineHeight: 1.6 }}>
                直播不受影响。如需显示节目单，请在设置中配置 XMLTV 地址。
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
