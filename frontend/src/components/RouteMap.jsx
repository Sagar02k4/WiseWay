import { MapContainer, TileLayer, Marker, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

function FitRoute({ points }) { const map = useMap(); if (points.length) map.fitBounds(points, { padding: [24, 24] }); return null; }
export default function RouteMap({ data, selectedMode, transitJourney }) {
  if (transitJourney) {
    try {
      const legs = transitJourney.legGeometries.map(x => JSON.parse(x).coordinates || []).filter(x => x.length);
      const points = legs.flat().map(([lon, lat]) => [lat, lon]);
      if (!points.length) return <div className="route-map-placeholder">Mapped stops are available, but this transit journey has no usable geometry.</div>;
      return <div className="route-map" aria-label="Mapped transit route"><MapContainer center={points[0]} zoom={12} scrollWheelZoom={false}><TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>{legs.map((leg,i)=><Polyline key={i} positions={leg.map(([lon,lat])=>[lat,lon])} pathOptions={{color:i?'#d36b3d':'#176b50',weight:5}}/>)}<Marker position={points[0]} icon={L.divIcon({className:'route-pin origin-pin',html:'●'})}/><Marker position={points[points.length-1]} icon={L.divIcon({className:'route-pin destination-pin',html:'●'})}/><FitRoute points={points}/></MapContainer><div className="route-map-meta"><b>{transitJourney.mode}</b><span>·</span><b>{transitJourney.journeyType.replace('_',' ')}</b><small>{transitJourney.source}</small></div></div>;
    } catch { return <div className="route-map-placeholder">Transit geometry could not be displayed.</div>; }
  }
  if (selectedMode && !['CAB', 'AUTO'].includes(selectedMode)) return <div className="route-map-placeholder">Route map unavailable for this transport option. Verified transit route geometry is not configured.</div>;
  if (!data?.routeGeometry || !data.routeDistanceMetres) return <div className="route-map-placeholder">A route map is available only when the routing provider returns route geometry.</div>;
  let coords; try { coords = JSON.parse(data.routeGeometry).coordinates; } catch { return <div className="route-map-placeholder">Route geometry could not be displayed.</div>; }
  const points = coords.map(([lon, lat]) => [lat, lon]); const origin = points[0]; const destination = points[points.length - 1];
  return <div className="route-map" aria-label="Calculated road route"><MapContainer center={origin} zoom={12} scrollWheelZoom={false}><TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/><Polyline positions={points} pathOptions={{ color: '#176b50', weight: 5 }}/><Marker position={origin} icon={L.divIcon({className:'route-pin origin-pin',html:'●'})}/><Marker position={destination} icon={L.divIcon({className:'route-pin destination-pin',html:'●'})}/><FitRoute points={points}/></MapContainer><div className="route-map-meta"><b>{(data.routeDistanceMetres / 1000).toFixed(1)} km</b><span>·</span><b>{Math.round(data.routeDurationSeconds / 60)} min estimated</b><small>{data.routeSource}</small></div></div>;
}
