import { useState } from 'react';
import { Search, Settings, RefreshCw, Star, Play, Wifi, WifiOff, Loader2, AlertCircle } from 'lucide-react';
import { CHANNELS, NAV_SECTIONS, Channel, SyncStatus, DEMO_EPG } from './tvData';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';

interface HomeScreenProps {
  navigate: (screen: Screen, channel?: Channel) => void;
  currentTime: string;
  currentDate: string;
  syncStatus: SyncStatus;
}

// ─── Shared atoms ──────────────────────────────────────────────

export function ChannelLogo({ ch, size = 48 }: { ch: Channel; size?: number }) {
  const r = Math.round(size * 0.14);
  return (
    <div style={{
      width: size, height: size, borderRadius: r, flexShrink: 0,
      background: `linear-gradient(135deg, ${ch.logoColor}dd 0%, ${ch.logoColor}77 100%)`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      <span style={{ color: '#fff', fontSize: size * 0.22, fontWeight: 800, letterSpacing: 0 }}>
        {ch.logoText}
      </span>
    </div>
  );
}

export function LiveBadge() {
  return (
    <span style={{
      background: '#e53535', color: '#fff',
      fontSize: 10, fontWeight: 700, letterSpacing: 0.8,
      padding: '2px 7px', borderRadius: 3,
    }}>
      直播
    </span>
  );
}

export function QualityBadge({ q }: { q?: string }) {
  if (!q) return null;
  const color = q === '4K' ? '#d4a017' : '#6b7fa3';
  return (
    <span style={{
      color, fontSize: 10, fontWeight: 600,
      border: `1px solid ${color}55`,
      padding: '2px 6px', borderRadius: 3,
    }}>
      {q}
    </span>
  );
}

export function CategoryChip({ label }: { label: string }) {
  return (
    <span style={{
      color: '#8899b4', fontSize: 10,
      border: '1px solid rgba(255,255,255,0.1)',
      padding: '2px 7px', borderRadius: 3,
    }}>
      {label}
    </span>
  );
}

// ─── Sync status ────────────────────────────────────────────────

function SyncIndicator({ status }: { status: SyncStatus }) {
  const map: Record<SyncStatus, { color: string; icon: React.ReactNode; label: string }> = {
    syncing:  { color: '#00c8d4', icon: <Loader2 size={12} style={{ animation: 'spin 1s linear infinite' }} />, label: '正在同步' },
    synced:   { color: '#22c55e', icon: <Wifi size={12} />, label: '已同步' },
    failed:   { color: '#e53535', icon: <WifiOff size={12} />, label: '同步失败' },
    pending:  { color: '#6b7fa3', icon: <AlertCircle size={12} />, label: '等待同步' },
  };
  const { color, icon, label } = map[status];
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5, color }}>
      {icon}
      <span style={{ fontSize: 11, fontWeight: 500 }}>{label}</span>
    </div>
  );
}

// ─── Channel card ───────────────────────────────────────────────

function ChannelCard({
  ch, focused, onClick, onFocus, onBlur,
}: {
  ch: Channel; focused: boolean;
  onClick: () => void; onFocus: () => void; onBlur: () => void;
}) {
  return (
    <div
      tabIndex={0}
      onFocus={onFocus}
      onBlur={onBlur}
      onClick={onClick}
      style={{
        width: 184,
        background: focused ? '#1c2233' : '#13171f',
        border: `1px solid ${focused ? '#00c8d4' : 'rgba(255,255,255,0.06)'}`,
        borderRadius: 8,
        padding: '14px 14px 12px',
        cursor: 'pointer',
        outline: 'none',
        transition: 'all 0.14s ease',
        transform: focused ? 'scale(1.05)' : 'scale(1)',
        boxShadow: focused
          ? '0 0 0 1px #00c8d4, 0 8px 32px rgba(0,200,212,0.2), 0 4px 16px rgba(0,0,0,0.55)'
          : '0 2px 8px rgba(0,0,0,0.3)',
        flexShrink: 0,
        display: 'flex', flexDirection: 'column', gap: 8,
      }}
    >
      {/* Logo + name row */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <ChannelLogo ch={ch} size={40} />
        <div style={{ minWidth: 0 }}>
          <div style={{ color: '#dde4f0', fontSize: 12, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {ch.shortName}
          </div>
          <div style={{ color: '#6b7fa3', fontSize: 10, marginTop: 1 }}>{ch.categoryLabel}</div>
        </div>
        {ch.isFavorite && <Star size={11} style={{ color: '#d4a017', fill: '#d4a017', marginLeft: 'auto', flexShrink: 0 }} />}
      </div>
      {/* Badges */}
      <div style={{ display: 'flex', gap: 5, alignItems: 'center', flexWrap: 'wrap' }}>
        <LiveBadge />
        <QualityBadge q={ch.quality} />
        <span style={{ color: '#6b7fa3', fontSize: 10, marginLeft: 'auto' }}>
          {ch.sourceCount} 源
        </span>
      </div>
    </div>
  );
}

// ─── Hero card ──────────────────────────────────────────────────

function HeroCard({
  ch, focused, onClick, onFocus, onBlur,
}: {
  ch: Channel; focused: boolean;
  onClick: () => void; onFocus: () => void; onBlur: () => void;
}) {
  const epg = DEMO_EPG[ch.id];
  const currentProg = epg?.find(p => p.status === 'current');

  return (
    <div
      tabIndex={0}
      onFocus={onFocus}
      onBlur={onBlur}
      onClick={onClick}
      style={{
        width: 540, height: 304,
        background: `linear-gradient(140deg, ${ch.logoColor}2a 0%, #0f1420 65%)`,
        border: `1px solid ${focused ? '#00c8d4' : 'rgba(255,255,255,0.07)'}`,
        borderRadius: 8,
        position: 'relative', overflow: 'hidden',
        cursor: 'pointer', outline: 'none',
        transition: 'all 0.14s ease',
        transform: focused ? 'scale(1.015)' : 'scale(1)',
        boxShadow: focused
          ? '0 0 0 2px #00c8d4, 0 12px 48px rgba(0,200,212,0.2)'
          : '0 4px 24px rgba(0,0,0,0.45)',
        flexShrink: 0,
      }}
    >
      {/* subtle ambient glow */}
      <div style={{
        position: 'absolute', inset: 0,
        background: `radial-gradient(ellipse at 25% 50%, ${ch.logoColor}18 0%, transparent 55%)`,
        pointerEvents: 'none',
      }} />
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(to top, #0d0f12 0%, transparent 55%)',
        pointerEvents: 'none',
      }} />

      {/* Large logo watermark */}
      <div style={{
        position: 'absolute', top: '50%', left: '50%',
        transform: 'translate(-50%, -50%)',
        opacity: 0.07,
      }}>
        <ChannelLogo ch={ch} size={180} />
      </div>

      {/* Top badges */}
      <div style={{ position: 'absolute', top: 16, left: 16, display: 'flex', gap: 8, alignItems: 'center' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 5,
          background: 'rgba(229,53,53,0.88)', borderRadius: 4, padding: '3px 9px',
        }}>
          <div style={{ width: 5, height: 5, background: '#fff', borderRadius: '50%', animation: 'pulse 1.5s ease infinite' }} />
          <span style={{ color: '#fff', fontSize: 10, fontWeight: 700, letterSpacing: 0.8 }}>直播中</span>
        </div>
        <QualityBadge q={ch.quality} />
      </div>

      {/* Play hint */}
      {focused && (
        <div style={{
          position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            width: 52, height: 52, borderRadius: '50%',
            background: 'rgba(0,200,212,0.2)', border: '2px solid #00c8d4',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Play size={20} style={{ color: '#00c8d4', marginLeft: 3 }} />
          </div>
        </div>
      )}

      {/* Bottom info */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '14px 18px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
          <ChannelLogo ch={ch} size={36} />
          <div>
            <div style={{ color: '#fff', fontSize: 17, fontWeight: 700 }}>{ch.name}</div>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginTop: 2 }}>
              <CategoryChip label={ch.categoryLabel} />
              <span style={{ color: '#6b7fa3', fontSize: 10 }}>{ch.sourceCount} 个播放源</span>
            </div>
          </div>
        </div>

        {/* EPG line — only shown when data exists */}
        {currentProg && (
          <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid rgba(255,255,255,0.07)' }}>
            <div style={{ color: '#6b7fa3', fontSize: 10, marginBottom: 3 }}>
              {currentProg.time}–{currentProg.end}
            </div>
            <div style={{ color: '#b8c4d8', fontSize: 12, fontWeight: 500, marginBottom: 6 }}>
              {currentProg.name}
            </div>
            {currentProg.progress != null && (
              <div style={{ height: 2, background: 'rgba(255,255,255,0.1)', borderRadius: 1 }}>
                <div style={{ width: `${currentProg.progress}%`, height: '100%', background: '#00c8d4', borderRadius: 1 }} />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main screen ─────────────────────────────────────────────────

export function HomeScreen({ navigate, currentTime, currentDate, syncStatus }: HomeScreenProps) {
  const [activeNav, setActiveNav] = useState('recent');
  const [focusedCardId, setFocusedCardId] = useState<string | null>(null);
  const [heroFocused, setHeroFocused] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);
  const [refreshFocused, setRefreshFocused] = useState(false);
  const [settingsFocused, setSettingsFocused] = useState(false);

  const heroChannel = CHANNELS.find(c => c.id === 'cctv5') || CHANNELS[0];

  const filtered = (() => {
    if (activeNav === 'recent') return CHANNELS.slice(0, 8);
    if (activeNav === 'favorite') return CHANNELS.filter(c => c.isFavorite);
    if (activeNav === 'all') return CHANNELS;
    return CHANNELS.filter(c => c.categoryId === activeNav);
  })();

  const row1 = filtered.slice(0, 7);
  const row2 = filtered.slice(7, 14);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* ── Top bar ── */}
      <div style={{
        height: 54, display: 'flex', alignItems: 'center', padding: '0 28px',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        background: '#0f1218', flexShrink: 0, gap: 18,
      }}>
        {/* Brand icon only */}
        <div style={{
          width: 34, height: 34, borderRadius: 8,
          background: 'linear-gradient(135deg, #00c8d4, #0080a0)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          <span style={{ fontSize: 18, lineHeight: 1 }}>🐋</span>
        </div>

        <div style={{ width: 1, height: 22, background: 'rgba(255,255,255,0.08)' }} />

        {/* Clock */}
        <div>
          <div style={{ color: '#dde4f0', fontSize: 16, fontWeight: 600, lineHeight: 1.1, fontVariantNumeric: 'tabular-nums', fontFamily: 'JetBrains Mono, monospace' }}>
            {currentTime}
          </div>
          <div style={{ color: '#6b7fa3', fontSize: 10 }}>{currentDate}</div>
        </div>

        {/* Spacer */}
        <div style={{ flex: 1 }} />

        {/* Sync status */}
        <SyncIndicator status={syncStatus} />

        <div style={{ width: 1, height: 18, background: 'rgba(255,255,255,0.08)' }} />

        {/* Icon buttons */}
        {[
          { icon: <Search size={17} />, focused: searchFocused, setFocused: setSearchFocused, onClick: () => navigate('search'), title: '搜索' },
          { icon: <RefreshCw size={16} />, focused: refreshFocused, setFocused: setRefreshFocused, onClick: () => {}, title: '刷新' },
          { icon: <Settings size={17} />, focused: settingsFocused, setFocused: setSettingsFocused, onClick: () => navigate('settings'), title: '设置' },
        ].map(btn => (
          <button
            key={btn.title}
            tabIndex={0}
            title={btn.title}
            onFocus={() => btn.setFocused(true)}
            onBlur={() => btn.setFocused(false)}
            onClick={btn.onClick}
            style={{
              background: btn.focused ? 'rgba(0,200,212,0.1)' : 'transparent',
              border: `1px solid ${btn.focused ? '#00c8d4' : 'transparent'}`,
              borderRadius: 7, padding: 8, cursor: 'pointer', outline: 'none',
              color: btn.focused ? '#00c8d4' : '#6b7fa3',
              boxShadow: btn.focused ? '0 0 0 1px #00c8d4' : 'none',
              transition: 'all 0.1s',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            {btn.icon}
          </button>
        ))}
      </div>

      {/* ── Body ── */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Left nav */}
        <nav style={{
          width: 148, flexShrink: 0,
          background: '#0f1218',
          borderRight: '1px solid rgba(255,255,255,0.05)',
          padding: '14px 0', overflowY: 'auto',
          display: 'flex', flexDirection: 'column', gap: 1,
        }}>
          {NAV_SECTIONS.map(s => {
            const active = activeNav === s.id;
            return (
              <button
                key={s.id}
                tabIndex={0}
                onClick={() => setActiveNav(s.id)}
                onFocus={() => setActiveNav(s.id)}
                style={{
                  display: 'block', width: '100%', textAlign: 'left',
                  padding: '9px 16px',
                  background: active ? 'rgba(0,200,212,0.07)' : 'transparent',
                  borderLeft: `3px solid ${active ? '#00c8d4' : 'transparent'}`,
                  border: 'none',
                  borderLeftStyle: 'solid',
                  borderLeftWidth: 3,
                  borderLeftColor: active ? '#00c8d4' : 'transparent',
                  color: active ? '#00c8d4' : '#6b7fa3',
                  fontSize: 13, fontWeight: active ? 600 : 400,
                  cursor: 'pointer', outline: 'none',
                  transition: 'all 0.1s',
                  fontFamily: 'Noto Sans SC, sans-serif',
                }}
              >
                {s.label}
              </button>
            );
          })}
        </nav>

        {/* Content */}
        <div style={{ flex: 1, padding: '22px 26px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 22 }}>
          {/* Hero row */}
          <div style={{ display: 'flex', gap: 22, alignItems: 'flex-start' }}>
            <HeroCard
              ch={heroChannel}
              focused={heroFocused}
              onFocus={() => setHeroFocused(true)}
              onBlur={() => setHeroFocused(false)}
              onClick={() => navigate('player', heroChannel)}
            />

            {/* Side info panel */}
            <div style={{ flex: 1, paddingTop: 4, display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <div style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 6 }}>已选频道</div>
                <div style={{ color: '#dde4f0', fontSize: 20, fontWeight: 700, marginBottom: 3 }}>{heroChannel.name}</div>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginTop: 4 }}>
                  <LiveBadge />
                  <CategoryChip label={heroChannel.categoryLabel} />
                  {heroChannel.quality && <QualityBadge q={heroChannel.quality} />}
                </div>
              </div>

              <div style={{ height: 1, background: 'rgba(255,255,255,0.05)' }} />

              {/* Sources */}
              <div>
                <div style={{ color: '#6b7fa3', fontSize: 10, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 8 }}>播放源</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  {heroChannel.sources.map((src, i) => {
                    const statusColor = { ok: '#22c55e', fair: '#d4a017', unavailable: '#e53535', unchecked: '#6b7fa3' }[src.status];
                    const statusLabel = { ok: '可用', fair: '一般', unavailable: '不可用', unchecked: '待检测' }[src.status];
                    return (
                      <div key={i} style={{
                        display: 'flex', alignItems: 'center', gap: 10,
                        background: '#13171f', border: '1px solid rgba(255,255,255,0.06)',
                        borderRadius: 5, padding: '7px 12px',
                      }}>
                        <div style={{ width: 6, height: 6, borderRadius: '50%', background: statusColor, flexShrink: 0 }} />
                        <span style={{ color: '#b8c4d8', fontSize: 12, flex: 1 }}>{src.label}</span>
                        {src.quality && <span style={{ color: '#6b7fa3', fontSize: 10 }}>{src.quality}</span>}
                        <span style={{ color: statusColor, fontSize: 11 }}>{statusLabel}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div style={{ height: 1, background: 'rgba(255,255,255,0.05)' }} />

              {/* Actions */}
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  tabIndex={0}
                  onClick={() => navigate('player', heroChannel)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 7,
                    background: '#00c8d4', color: '#0d0f12', border: 'none',
                    borderRadius: 6, padding: '9px 18px',
                    fontSize: 13, fontWeight: 700, cursor: 'pointer', outline: 'none',
                  }}
                >
                  <Play size={14} style={{ fill: '#0d0f12' }} /> 立即播放
                </button>
                <button
                  tabIndex={0}
                  onClick={() => navigate('epg', heroChannel)}
                  style={{
                    background: 'rgba(255,255,255,0.05)', color: '#dde4f0',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 6, padding: '9px 16px', fontSize: 13,
                    cursor: 'pointer', outline: 'none',
                  }}
                >
                  频道详情
                </button>
              </div>
            </div>
          </div>

          {/* Row label */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: -8 }}>
            <span style={{ color: '#dde4f0', fontSize: 13, fontWeight: 600 }}>
              {NAV_SECTIONS.find(s => s.id === activeNav)?.label}
            </span>
            <span style={{ color: '#6b7fa3', fontSize: 11 }}>{filtered.length} 个频道</span>
          </div>

          {/* Row 1 */}
          <div style={{ display: 'flex', gap: 10, overflow: 'hidden' }}>
            {row1.map(ch => (
              <ChannelCard
                key={ch.id} ch={ch}
                focused={focusedCardId === ch.id}
                onFocus={() => setFocusedCardId(ch.id)}
                onBlur={() => setFocusedCardId(null)}
                onClick={() => navigate('player', ch)}
              />
            ))}
          </div>

          {/* Row 2 */}
          {row2.length > 0 && (
            <div style={{ display: 'flex', gap: 10, overflow: 'hidden' }}>
              {row2.map(ch => (
                <ChannelCard
                  key={ch.id} ch={ch}
                  focused={focusedCardId === ch.id}
                  onFocus={() => setFocusedCardId(ch.id)}
                  onBlur={() => setFocusedCardId(null)}
                  onClick={() => navigate('player', ch)}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.25} }
        @keyframes spin  { to{transform:rotate(360deg)} }
      `}</style>
    </div>
  );
}
