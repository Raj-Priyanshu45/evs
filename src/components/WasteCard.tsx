import { ArrowUpRight, Recycle } from 'lucide-react'
import type { WasteItem } from '../data/wasteItems'

type Props = { item: WasteItem; onOpen: (item: WasteItem) => void }

export default function WasteCard({ item, onOpen }: Props) {
  return (
    <button className="waste-card" onClick={() => onOpen(item)} aria-label={`View disposal instructions for ${item.name}`}>
      <div className={`item-icon icon-${item.categorySlug}`} aria-hidden="true">{item.icon}</div>
      <div className="card-heading-row">
        <span className={`category-tag tag-${item.categorySlug}`}>{item.category}</span>
        <span className="card-open-icon"><ArrowUpRight size={17} strokeWidth={1.8} /></span>
      </div>
      <h3>{item.name}</h3>
      <p className="card-description">{item.description}</p>
      <div className="card-foot">
        <span className="recycle-label"><Recycle size={14} /> {item.recyclability}</span>
        <span className="card-link">View guide</span>
      </div>
    </button>
  )
}
