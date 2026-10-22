import { useEffect } from 'react'
import { Check, X, Leaf, ShieldCheck, X as CloseIcon } from 'lucide-react'
import type { WasteItem } from '../data/wasteItems'

type Props = { item: WasteItem; onClose: () => void }

export default function ItemDetails({ item, onClose }: Props) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKeyDown)
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = ''
    }
  }, [onClose])

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="details-modal" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <button className="modal-close" onClick={onClose} aria-label="Close disposal guide"><CloseIcon size={19} /></button>
        <div className="detail-topline"><span className={`category-tag tag-${item.categorySlug}`}>{item.category}</span><span className="detail-step">DISPOSAL GUIDE / 01</span></div>
        <div className={`detail-hero-icon icon-${item.categorySlug}`} aria-hidden="true">{item.icon}</div>
        <h2 id="detail-title">{item.name}</h2>
        <p className="detail-description">{item.description}</p>
        <div className="disposal-banner">
          <div className="disposal-symbol"><ShieldCheck size={21} /></div>
          <div><span className="eyebrow">BEST NEXT STEP</span><strong>{item.disposalType}</strong><small>{item.recyclability}</small></div>
        </div>
        <div className="detail-columns">
          <div className="detail-section">
            <h3><Check size={16} /> How to dispose</h3>
            <ol>{item.instructions.map((instruction, index) => <li key={instruction}><span>{String(index + 1).padStart(2, '0')}</span>{instruction}</li>)}</ol>
          </div>
          <div className="detail-section avoid-section">
            <h3><X size={16} /> Avoid this</h3>
            <ul>{item.avoid.map((warning) => <li key={warning}>{warning}</li>)}</ul>
          </div>
        </div>
        <div className="quick-tip"><Leaf size={18} /><p><strong>WasteWise tip</strong>{item.tip}</p></div>
        <p className="local-disclaimer">Disposal rules vary by city and collection provider. Use this as a starter guide and confirm local instructions for hazardous or specialist waste.</p>
      </section>
    </div>
  )
}
