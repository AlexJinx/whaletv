import { useState, useEffect } from 'react';
import { HomeScreen } from './components/HomeScreen';
import { PlayerScreen } from './components/PlayerScreen';
import { EPGScreen } from './components/EPGScreen';
import { SearchScreen } from './components/SearchScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { Channel, CHANNELS, SyncStatus } from './components/tvData';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';

function useCurrentTime() {
  const fmt = (d: Date) => ({
    time: d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    date: d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' }),
  });
  const [state, setState] = useState(() => fmt(new Date()));
  useEffect(() => {
    const id = setInterval(() => setState(fmt(new Date())), 15000);
    return () => clearInterval(id);
  }, []);
  return state;
}

function useTVScale() {
  const [scale, setScale] = useState(1);
  useEffect(() => {
    const update = () => setScale(Math.min(window.innerWidth / 1920, window.innerHeight / 1080));
    update();
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, []);
  return scale;
}

// Screen labels for the demo tab bar
const SCREEN_LABELS: Record<Screen, string> = {
  home: '主页', player: '播放器', epg: '频道详情', search: '搜索', settings: '设置',
};

export default function App() {
  const [screen, setScreen] = useState<Screen>('home');
  const [activeChannel, setActiveChannel] = useState<Channel>(CHANNELS[4]); // CCTV-5
  const [syncStatus] = useState<SyncStatus>('synced');
  const { time, date } = useCurrentTime();
  const scale = useTVScale();

  const navigate = (target: Screen, channel?: Channel) => {
    if (channel) setActiveChannel(channel);
    setScreen(target);
  };

  return (
    <div style={{ width: '100vw', height: '100vh', background: '#040508', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
      {/* 1920×1080 TV canvas */}
      <div
        className="dark"
        style={{
          width: 1920, height: 1080,
          transformOrigin: 'center center',
          transform: `scale(${scale})`,
          background: '#0d0f12',
          position: 'relative', overflow: 'hidden',
          fontFamily: "'Noto Sans SC', 'Inter', system-ui, sans-serif",
        }}
      >
        {/* Demo navigation tabs */}
        <div style={{ position: 'absolute', top: 0, right: 0, zIndex: 9999, display: 'flex' }}>
          {(Object.keys(SCREEN_LABELS) as Screen[]).map(s => (
            <button
              key={s}
              onClick={() => setScreen(s)}
              style={{
                background: screen === s ? 'rgba(0,200,212,0.9)' : 'rgba(5,8,14,0.85)',
                color: screen === s ? '#0d0f12' : '#6b7fa3',
                border: 'none', borderBottom: '1px solid rgba(255,255,255,0.08)', borderLeft: '1px solid rgba(255,255,255,0.06)',
                padding: '4px 11px', fontSize: 10, fontWeight: 600,
                cursor: 'pointer', letterSpacing: 0.5,
              }}
            >
              {SCREEN_LABELS[s]}
            </button>
          ))}
        </div>

        {screen === 'home' && (
          <HomeScreen navigate={navigate} currentTime={time} currentDate={date} syncStatus={syncStatus} />
        )}
        {screen === 'player' && (
          <PlayerScreen navigate={navigate} channel={activeChannel} currentTime={time} />
        )}
        {screen === 'epg' && (
          <EPGScreen navigate={navigate} channel={activeChannel} />
        )}
        {screen === 'search' && (
          <SearchScreen navigate={navigate} />
        )}
        {screen === 'settings' && (
          <SettingsScreen navigate={navigate} />
        )}
      </div>
    </div>
  );
}
