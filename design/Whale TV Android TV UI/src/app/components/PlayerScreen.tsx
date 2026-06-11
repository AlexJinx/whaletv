import { useState } from 'react';
import { ArrowLeft, Star, Loader2, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { CHANNELS, Channel, DEMO_EPG } from './tvData';
import { ChannelLogo, LiveBadge, QualityBadge } from './HomeScreen';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';
type PlayState = 'connecting' | 'buffering' | 'live' | 'switching';

interface PlayerScreenProps {
  navigate: (screen: Screen, channel?: Channel) => void;
  channel: Channel;
  currentTime: string;
}

function PlayStateChip({ state }: { state: PlayState }) {
  const map: Record<PlayState, { color: string; icon: React.ReactNode; label: string }> = {
    connecting: { color: '#d4a017', icon: <Loader2 size={11} style={{ animation: 'spin 1s linear infinite' }} />, label: '正在连接' },
    buffering:  { color: '#d4a017', icon: <Loader2 size={11} style={{ animation: 'spin 1s linear infinite' }} />, label: '缓冲中' },
    live:       { color: '#e53535', icon: <div style={{ width: 5, height: 5, background: '#fff', borderRadius: '50%', animation: 'pulse 1.5s ease infinite' }} />, label: '直播中' },
    switching:  { color: '#d4a017', icon: <AlertTriangle size={11} />, label: '当前源失败，正在切换备用源' },
  };
  const { color, icon, label } = map[state];
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 5,
      background: `${color}22`, border: `1px solid ${color}55`,
      borderRadius: 4, padding: '3px 9px', backdropFilter: 'blur(6px)',
    }}>
      <span style={{ color, display: 'flex' }}>{icon}</span>
      <span style={{ color, fontSize: 11, fontWeight: 700 }}>{label}</span>
    </div>
  );
}

function CarouselCard({ ch, focused, onClick }: { ch: Channel; focused: boolean; onClick: () => void }) {
  const w = focused ? 140 : 110;
  const h = focused ? 80 : 62;
  return (
    <div
      tabIndex={0}
      onClick={onClick}
      style={{
        width: w, height: h, flexShrink: 0,
        background: focused ? 'rgba(0,200,212,0.1)' : 'rgba(10,14,20,0.78)',
        border: `1px solid ${focused ? '#00c8d4' : 'rgba(255,255,255,0.08)'}`,
        borderRadius: 6, padding: '9px 10px',
        cursor: 'pointer', outline: 'none',
        backdropFilter: 'blur(12px)',
        boxShadow: focused ? '0 0 0 1px #00c8d4, 0 4px 20px rgba(0,200,212,0.22)' : 'none',
        transition: 'all 0.15s ease',
        display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
        <ChannelLogo ch={ch} size={focused ? 26 : 20} />
        <span style={{ color: '#dde4f0', fontSize: focused ? 11 : 10, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {ch.shortName}
        </span>
      </div>
      {focused && (
        <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
          <LiveBadge />
          {ch.quality && <QualityBadge q={ch.quality} />}
          <span style={{ color: '#6b7fa3', fontSize: 9, marginLeft: 'auto' }}>{ch.sourceCount}源</span>
        </div>
      )}
    </div>
  );
}

export function PlayerScreen({ navigate, channel, currentTime }: PlayerScreenProps) {
  const [isFavorite, setIsFavorite] = useState(channel.isFavorite);
  const [playState, setPlayState] = useState<PlayState>('live');
  const [activeSourceIdx, setActiveSourceIdx] = useState(0);
  const [focusedCarouselIdx, setFocusedCarouselIdx] = useState(
    CHANNELS.findIndex(c => c.id === channel.id)
  );
  const [backFocused, setBackFocused] = useState(false);
  const [favFocused, setFavFocused] = useState(false);

  const carousel = CHANNELS.slice(0, 9);
  const epg = DEMO_EPG[channel.id];
  const currentProg = epg?.find(p => p.status === 'current');
  const nextProg = epg?.find(p => p.status === 'next');

  const activeSource = channel.sources[activeSourceIdx];

  // Cycle playback state for demo
  const STATES: PlayState[] = ['live', 'buffering', 'connecting', 'switching'];
  const cycleState = () => setPlayState(s => STATES[(STATES.indexOf(s) + 1) % STATES.length]);

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative', background: '#060a10', overflow: 'hidden' }}>
      {/* Simulated video bg */}
      <div style={{
        position: 'absolute', inset: 0,
        background: `
          radial-gradient(ellipse at 25% 45%, ${channel.logoColor}14 0%, transparent 48%),
          radial-gradient(ellipse at 75% 30%, rgba(0,80,120,0.12) 0%, transparent 45%),
          linear-gradient(150deg, #080c14 0%, #0b1020 100%)
        `,
      }} />
      {/* Scanline */}
      <div style={{
        position: 'absolute', inset: 0, opacity: 0.025, pointerEvents: 'none',
        backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255,255,255,1) 2px, rgba(255,255,255,1) 3px)',
      }} />
      {/* Large logo watermark */}
      <div style={{
        position: 'absolute', top: '50%', left: '50%',
        transform: 'translate(-50%,-50%)', opacity: 0.04, pointerEvents: 'none',
      }}>
        <ChannelLogo ch={channel} size={320} />
      </div>

      {/* ── Top overlay ── */}
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0,
        background: 'linear-gradient(to bottom, rgba(6,10,16,0.95) 0%, transparent 100%)',
        padding: '16px 26px 40px',
        display: 'flex', alignItems: 'center', gap: 14,
      }}>
        <button
          tabIndex={0}
          onFocus={() => setBackFocused(true)}
          onBlur={() => setBackFocused(false)}
          onClick={() => navigate('home')}
          style={{
            background: backFocused ? 'rgba(0,200,212,0.12)' : 'rgba(255,255,255,0.07)',
            border: `1px solid ${backFocused ? '#00c8d4' : 'rgba(255,255,255,0.1)'}`,
            borderRadius: 6, padding: '6px 12px', cursor: 'pointer', outline: 'none',
            color: backFocused ? '#00c8d4' : '#dde4f0',
            display: 'flex', alignItems: 'center', gap: 6, fontSize: 13,
            boxShadow: backFocused ? '0 0 0 1px #00c8d4' : 'none',
            transition: 'all 0.1s', backdropFilter: 'blur(8px)',
          }}
        >
          ← 返回
        </button>

        <ChannelLogo ch={channel} size={30} />
        <div>
          <div style={{ color: '#dde4f0', fontSize: 15, fontWeight: 600 }}>{channel.name}</div>
          {/* EPG program line — only if data exists */}
          {currentProg && (
            <div style={{ color: '#8899b4', fontSize: 11, marginTop: 1 }}>{currentProg.name}</div>
          )}
        </div>

        <div style={{ flex: 1 }} />

        {/* Playback state */}
        <button onClick={cycleState} style={{ background: 'none', border: 'none', cursor: 'pointer', outline: 'none' }}>
          <PlayStateChip state={playState} />
        </button>

        {/* Active source */}
        <div style={{
          color: '#8899b4', fontSize: 11,
          background: 'rgba(13,15,18,0.6)', backdropFilter: 'blur(6px)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: 4, padding: '3px 9px',
        }}>
          {activeSource?.label} / {channel.sourceCount}
          {activeSource?.quality && <span style={{ marginLeft: 6, color: '#6b7fa3' }}>{activeSource.quality}</span>}
        </div>

        {/* Favorite */}
        <button
          tabIndex={0}
          onFocus={() => setFavFocused(true)}
          onBlur={() => setFavFocused(false)}
          onClick={() => setIsFavorite(v => !v)}
          style={{
            background: favFocused ? 'rgba(212,160,23,0.12)' : 'rgba(255,255,255,0.07)',
            border: `1px solid ${favFocused ? '#d4a017' : isFavorite ? '#d4a01755' : 'rgba(255,255,255,0.1)'}`,
            borderRadius: 6, padding: 8, cursor: 'pointer', outline: 'none',
            boxShadow: favFocused ? '0 0 0 1px #d4a017' : 'none',
            transition: 'all 0.1s', backdropFilter: 'blur(8px)',
          }}
        >
          <Star size={16} style={{ color: isFavorite ? '#d4a017' : '#6b7fa3', fill: isFavorite ? '#d4a017' : 'none' }} />
        </button>

        {/* Time */}
        <span style={{ color: '#6b7fa3', fontSize: 13, fontFamily: 'JetBrains Mono, monospace', fontVariantNumeric: 'tabular-nums' }}>
          {currentTime}
        </span>
      </div>

      {/* ── Right EPG panel — only when EPG data exists ── */}
      {epg && epg.length > 0 && (
        <div style={{
          position: 'absolute', top: 0, right: 0, bottom: 0, width: 248,
          background: 'rgba(8,12,20,0.88)', backdropFilter: 'blur(16px)',
          borderLeft: '1px solid rgba(255,255,255,0.06)',
          paddingTop: 72, paddingBottom: 100,
          display: 'flex', flexDirection: 'column',
        }}>
          <div style={{ padding: '0 18px 10px', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div style={{ color: '#6b7fa3', fontSize: 9, letterSpacing: 1, textTransform: 'uppercase' }}>节目单</div>
            <div style={{ color: '#dde4f0', fontSize: 12, fontWeight: 600, marginTop: 2 }}>{channel.name}</div>
          </div>
          <div style={{ flex: 1, overflowY: 'auto' }}>
            {epg.map((p, i) => (
              <div key={i} style={{
                padding: '9px 18px',
                borderLeft: `3px solid ${p.status === 'current' ? '#00c8d4' : 'transparent'}`,
                background: p.status === 'current' ? 'rgba(0,200,212,0.05)' : 'transparent',
                opacity: p.status === 'past' ? 0.4 : 1,
              }}>
                <div style={{ color: p.status === 'current' ? '#00c8d4' : '#6b7fa3', fontSize: 10, fontFamily: 'JetBrains Mono, monospace', marginBottom: 2 }}>
                  {p.time}
                </div>
                <div style={{ color: p.status === 'current' ? '#dde4f0' : '#8899b4', fontSize: 12, fontWeight: p.status === 'current' ? 600 : 400 }}>
                  {p.name}
                </div>
                {p.status === 'current' && p.progress != null && (
                  <div style={{ marginTop: 5, height: 2, background: 'rgba(255,255,255,0.08)', borderRadius: 1 }}>
                    <div style={{ width: `${p.progress}%`, height: '100%', background: '#00c8d4', borderRadius: 1 }} />
                  </div>
                )}
              </div>
            ))}
          </div>
          <div style={{ padding: '10px 18px', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
            <button
              tabIndex={0}
              onClick={() => navigate('epg', channel)}
              style={{
                width: '100%', background: 'rgba(0,200,212,0.08)', border: '1px solid rgba(0,200,212,0.25)',
                borderRadius: 6, padding: '7px 0', color: '#00c8d4', fontSize: 11, fontWeight: 600,
                cursor: 'pointer', outline: 'none',
              }}
            >
              查看完整节目单
            </button>
          </div>
        </div>
      )}

      {/* ── Bottom carousel ── */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: epg?.length ? 248 : 0,
        background: 'linear-gradient(to top, rgba(6,10,16,0.97) 0%, rgba(6,10,16,0.5) 75%, transparent 100%)',
        padding: '16px 26px 18px',
      }}>
        <div style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 0.5, marginBottom: 8 }}>其他频道</div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end', overflowX: 'hidden' }}>
          {carousel.map((ch, i) => (
            <CarouselCard
              key={ch.id} ch={ch}
              focused={focusedCarouselIdx === i}
              onClick={() => navigate('player', ch)}
            />
          ))}
        </div>
      </div>

      <style>{`
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.25} }
        @keyframes spin  { to{transform:rotate(360deg)} }
      `}</style>
    </div>
  );
}
