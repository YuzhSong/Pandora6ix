function tagTextColor(hex: string) {
  const channels = hex.slice(1).match(/.{2}/g)?.map(channel => {
    const value = parseInt(channel, 16) / 255
    return value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4
  }) ?? [0, 0, 0]
  const luminance = channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722
  return luminance > .179 ? '#000000' : '#FFFFFF'
}

export function TagChip({ name, color }: { name: string; color: string }) {
  return <span className="tag-chip" style={{ background: color, color: tagTextColor(color), borderColor: color }}>{name}</span>
}
