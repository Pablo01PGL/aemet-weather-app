import { Precipitation } from './precipitation';
import { TemperatureUnit } from './temperature-unit';

export interface Forecast {
  mediaTemperatura: number;
  unidadTemperatura: TemperatureUnit;
  probPrecipitacion: Precipitation[];
}
