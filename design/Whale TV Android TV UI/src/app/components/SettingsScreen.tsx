import { useState } from 'react';
import {
  ArrowLeft, Database, Clock, RefreshCw, Tv2, Trash2, Globe,
  ChevronRight, CheckCircle2, ToggleLeft, ToggleRight, Radio
} from 'lucide-react';

type Screen = 'home' | 'player' | 'epg' | 'search' | 'settings';
interface SettingsScreenProps { navigate: (screen: Screen) => void; }

const SECTIONS = [
  { id: 'source',   label: '播放源',   icon: <Database size={15} /> },
  { id: 'epg',      label: '节目单',   icon: <Clock size={15} /> },
  { id: 'refresh',  label: '刷新',     icon: <RefreshCw size={15} /> },
  { id: 'playback', label: '播放',     icon: <Radio size={15} /> },
  { id: 'startup',  label: '启动',     icon: <Tv2 size={15} /> },
  { id: 'cache',    label: '缓存',     icon: <Trash2 size={15} /> },
];

function Toggle({ on, onChange }: { on: boolean; onChange: () => void }) {
  return (
    <button
      tabIndex={0}
      onClick={onChange}
      style={{ background: 'none', border: 'none', cursor: 'pointer', outline: 'none', padding: 0, color: on ? '#00c8d4' : '#2a3a50' }}
    >
      {on ? <ToggleRight size={26} /> : <ToggleLeft size={26} />}
    </button>
  );
}

function Seg({ opts, val, onChange }: { opts: string[]; val: string; onChange: (v: string) => void }) {
  return (
    <div style={{ display: 'flex', gap: 2, background: '#0d0f12', borderRadius: 5, padding: 2, border: '1px solid rgba(255,255,255,0.06)' }}>
      {opts.map(o => (
        <button
          key={o}
          tabIndex={0}
          onClick={() => onChange(o)}
          style={{
            background: val === o ? 'rgba(0,200,212,0.85)' : 'transparent',
            color: val === o ? '#0d0f12' : '#6b7fa3',
            border: 'none', borderRadius: 4, padding: '5px 13px',
            cursor: 'pointer', outline: 'none',
            fontSize: 11, fontWeight: val === o ? 600 : 400, transition: 'all 0.09s',
          }}
        >
          {o}
        </button>
      ))}
    </div>
  );
}

function Row({
  label, sub, children, onClick, focused, onFocus, onBlur,
}: {
  label: string; sub?: string; children?: React.ReactNode;
  onClick?: () => void; focused?: boolean; onFocus?: () => void; onBlur?: () => void;
}) {
  return (
    <div
      tabIndex={onClick ? 0 : -1}
      onClick={onClick}
      onFocus={onFocus}
      onBlur={onBlur}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '13px 16px',
        background: focused ? 'rgba(0,200,212,0.05)' : 'transparent',
        border: `1px solid ${focused ? 'rgba(0,200,212,0.2)' : 'transparent'}`,
        borderRadius: 6, outline: 'none', cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.09s',
      }}
    >
      <div>
        <div style={{ color: '#dde4f0', fontSize: 13, fontWeight: 500, fontFamily: 'Noto Sans SC, sans-serif' }}>{label}</div>
        {sub && <div style={{ color: '#6b7fa3', fontSize: 11, marginTop: 2, fontFamily: 'JetBrains Mono, monospace' }}>{sub}</div>}
      </div>
      {children}
    </div>
  );
}

function SectionHead({ label }: { label: string }) {
  return (
    <div style={{ color: '#6b7fa3', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', padding: '0 16px', marginBottom: 4, marginTop: 4 }}>
      {label}
    </div>
  );
}

function Divider() {
  return <div style={{ height: 1, background: 'rgba(255,255,255,0.04)', margin: '6px 16px' }} />;
}

export function SettingsScreen({ navigate }: SettingsScreenProps) {
  const [activeSection, setActiveSection] = useState('source');
  const [backFocused, setBackFocused] = useState(false);
  const [focusedRow, setFocusedRow] = useState<string | null>(null);

  // Settings state
  const [useBuiltinSource, setUseBuiltinSource] = useState(true);
  const [customM3U] = useState('');
  const [epgUrl] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [refreshInterval, setRefreshInterval] = useState('30分钟');
  const [hideUnavailable, setHideUnavailable] = useState(true);
  const [startWithLast, setStartWithLast] = useState(true);
  const [hwDecode, setHwDecode] = useState(true);

  const content: Record<string, React.ReactNode> = {
    source: (
      <>
        <SectionHead label="内置源" />
        <Row
          label="使用内置 iptv-org 中国频道源"
          sub="iptv-org/iptv · 自动更新"
          focused={focusedRow === 'builtin'}
          onFocus={() => setFocusedRow('builtin')}
          onBlur={() => setFocusedRow(null)}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {useBuiltinSource && (
              <span style={{ color: '#22c55e', fontSize: 11, display: 'flex', alignItems: 'center', gap: 4 }}>
                <CheckCircle2 size={11} /> 已启用
              </span>
            )}
            <Toggle on={useBuiltinSource} onChange={() => setUseBuiltinSource(v => !v)} />
          </div>
        </Row>

        <Divider />
        <SectionHead label="自定义源" />
        <Row
          label="自定义 M3U 地址"
          sub={customM3U || '未配置'}
          onClick={() => {}}
          focused={focusedRow === 'customM3U'}
          onFocus={() => setFocusedRow('customM3U')}
          onBlur={() => setFocusedRow(null)}
        >
          <ChevronRight size={14} style={{ color: '#3a4a60' }} />
        </Row>
      </>
    ),

    epg: (
      <>
        <SectionHead label="XMLTV 节目单" />
        <Row
          label="XMLTV EPG 地址"
          sub={epgUrl || '未配置 — 节目单将不显示'}
          onClick={() => {}}
          focused={focusedRow === 'epgUrl'}
          onFocus={() => setFocusedRow('epgUrl')}
          onBlur={() => setFocusedRow(null)}
        >
          <ChevronRight size={14} style={{ color: '#3a4a60' }} />
        </Row>
        <div style={{ margin: '10px 16px' }}>
          <div style={{
            background: '#13171f', border: '1px solid rgba(255,255,255,0.06)',
            borderRadius: 6, padding: '10px 14px',
          }}>
            <div style={{ color: '#6b7fa3', fontSize: 11, lineHeight: 1.6 }}>
              节目单为可选功能。未配置时直播正常可用，频道详情中将显示"暂无节目单数据"提示。
            </div>
          </div>
        </div>
      </>
    ),

    refresh: (
      <>
        <SectionHead label="自动刷新" />
        <Row
          label="启用自动刷新"
          sub="定期从源地址更新频道列表"
          focused={focusedRow === 'autoRefresh'}
          onFocus={() => setFocusedRow('autoRefresh')}
          onBlur={() => setFocusedRow(null)}
        >
          <Toggle on={autoRefresh} onChange={() => setAutoRefresh(v => !v)} />
        </Row>
        {autoRefresh && (
          <Row
            label="刷新间隔"
            focused={focusedRow === 'interval'}
            onFocus={() => setFocusedRow('interval')}
            onBlur={() => setFocusedRow(null)}
          >
            <Seg opts={['15分钟','30分钟','1小时','6小时']} val={refreshInterval} onChange={setRefreshInterval} />
          </Row>
        )}
        <Divider />
        <Row
          label="隐藏不可用频道"
          sub="仅显示播放源状态为可用的频道"
          focused={focusedRow === 'hideUnavail'}
          onFocus={() => setFocusedRow('hideUnavail')}
          onBlur={() => setFocusedRow(null)}
        >
          <Toggle on={hideUnavailable} onChange={() => setHideUnavailable(v => !v)} />
        </Row>
        <Divider />
        <SectionHead label="手动操作" />
        <Row
          label="立即刷新频道列表"
          onClick={() => {}}
          focused={focusedRow === 'refreshNow'}
          onFocus={() => setFocusedRow('refreshNow')}
          onBlur={() => setFocusedRow(null)}
        >
          <div style={{ display: 'flex', gap: 6, alignItems: 'center', color: '#00c8d4', fontSize: 12 }}>
            <RefreshCw size={13} /> 刷新
          </div>
        </Row>
      </>
    ),

    playback: (
      <>
        <SectionHead label="解码" />
        <Row
          label="硬件解码加速"
          sub="使用 GPU 解码提升播放性能"
          focused={focusedRow === 'hw'}
          onFocus={() => setFocusedRow('hw')}
          onBlur={() => setFocusedRow(null)}
        >
          <Toggle on={hwDecode} onChange={() => setHwDecode(v => !v)} />
        </Row>
      </>
    ),

    startup: (
      <>
        <SectionHead label="启动行为" />
        <Row
          label="启动时播放上次频道"
          sub="应用启动后自动恢复上次观看的频道"
          focused={focusedRow === 'startLast'}
          onFocus={() => setFocusedRow('startLast')}
          onBlur={() => setFocusedRow(null)}
        >
          <Toggle on={startWithLast} onChange={() => setStartWithLast(v => !v)} />
        </Row>
        <Row
          label="默认启动页"
          focused={focusedRow === 'startPage'}
          onFocus={() => setFocusedRow('startPage')}
          onBlur={() => setFocusedRow(null)}
        >
          <Seg opts={['主页','全部频道','收藏']} val="主页" onChange={() => {}} />
        </Row>
      </>
    ),

    cache: (
      <>
        <SectionHead label="清除数据" />
        {[
          { id: 'clrChannel', label: '清除频道缓存' },
          { id: 'clrAll', label: '清除所有缓存' },
        ].map(item => (
          <Row
            key={item.id}
            label={item.label}
            onClick={() => {}}
            focused={focusedRow === item.id}
            onFocus={() => setFocusedRow(item.id)}
            onBlur={() => setFocusedRow(null)}
          >
            <div style={{ color: '#e53535', fontSize: 12, display: 'flex', gap: 5, alignItems: 'center' }}>
              <Trash2 size={13} /> 清除
            </div>
          </Row>
        ))}
        <Divider />
        <div style={{ padding: '10px 16px' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            {[{ k: '版本', v: 'v2.4.1' }, { k: '源状态', v: '已同步' }].map(item => (
              <div key={item.k} style={{
                flex: 1, background: '#13171f', border: '1px solid rgba(255,255,255,0.06)',
                borderRadius: 6, padding: '10px 13px',
              }}>
                <div style={{ color: '#6b7fa3', fontSize: 10, marginBottom: 2 }}>{item.k}</div>
                <div style={{ color: '#dde4f0', fontSize: 13, fontWeight: 600, fontFamily: 'JetBrains Mono, monospace' }}>{item.v}</div>
              </div>
            ))}
          </div>
        </div>
      </>
    ),
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
        <span style={{ color: '#dde4f0', fontSize: 14, fontWeight: 600 }}>设置</span>
      </div>

      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Section nav */}
        <nav style={{
          width: 168, flexShrink: 0, background: '#0f1218',
          borderRight: '1px solid rgba(255,255,255,0.05)',
          padding: '14px 0', display: 'flex', flexDirection: 'column', gap: 1,
        }}>
          {SECTIONS.map(s => {
            const active = activeSection === s.id;
            return (
              <button
                key={s.id}
                tabIndex={0}
                onClick={() => setActiveSection(s.id)}
                onFocus={() => setActiveSection(s.id)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 9, width: '100%', textAlign: 'left',
                  padding: '9px 16px',
                  background: active ? 'rgba(0,200,212,0.07)' : 'transparent',
                  borderLeft: `3px solid ${active ? '#00c8d4' : 'transparent'}`,
                  border: 'none', borderLeftStyle: 'solid', borderLeftWidth: 3, borderLeftColor: active ? '#00c8d4' : 'transparent',
                  color: active ? '#00c8d4' : '#6b7fa3',
                  fontSize: 13, fontWeight: active ? 600 : 400,
                  cursor: 'pointer', outline: 'none', transition: 'all 0.09s',
                  fontFamily: 'Noto Sans SC, sans-serif',
                }}
              >
                <span style={{ opacity: active ? 1 : 0.55 }}>{s.icon}</span>
                {s.label}
              </button>
            );
          })}
        </nav>

        {/* Settings content */}
        <div style={{ flex: 1, padding: '22px 26px', overflowY: 'auto' }}>
          <div style={{ maxWidth: 580 }}>
            <div style={{ color: '#dde4f0', fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
              {SECTIONS.find(s => s.id === activeSection)?.label}
            </div>
            <div style={{ height: 1, background: 'rgba(255,255,255,0.05)', marginBottom: 12 }} />
            {content[activeSection]}
          </div>
        </div>
      </div>
    </div>
  );
}
